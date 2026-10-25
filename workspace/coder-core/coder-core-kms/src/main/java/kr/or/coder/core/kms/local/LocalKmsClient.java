package kr.or.coder.core.kms.local;

import kr.or.coder.core.kms.KmsClient;
import kr.or.coder.core.kms.KmsEncryptResult;
import kr.or.coder.core.kms.SigningKeyVersion;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 로컬 개발·테스트 전용 KMS.
 * - KEK: AES-256-GCM (설정 masterKey 또는 기동 시 생성)
 * - 서명키: RSA-2048, 프로세스 메모리에만 보관 (인스턴스 간 공유되지 않음)
 * 운영(prod) 프로파일에서는 KmsConfig가 기동을 거부한다.
 * @author ywkim
 * @since 2026.10.09
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.09  ywkim        최초 생성
 * </pre>
 */
@Slf4j
public class LocalKmsClient implements KmsClient {

    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final String KEK_VERSION = "v1";

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKey masterKey;
    private final Clock clock;
    private final Map<String, List<LocalSigningKey>> signingKeys = new ConcurrentHashMap<>();

    /**
     * 마스터 KEK를 초기화 (미설정 시 임의 키를 생성하며 재기동 후 복호화 불가)
     * @param masterKeyBase64 Base64로 인코딩된 32바이트 마스터 키 (비어 있으면 임의 생성)
     * @param clock 시간 기준 Clock
     * @throws IllegalStateException 마스터 키가 32바이트가 아닌 경우
     */
    public LocalKmsClient(String masterKeyBase64, Clock clock) {
        this.clock = clock;
        byte[] keyBytes;
        if (masterKeyBase64 == null || masterKeyBase64.isBlank()) {
            keyBytes = new byte[32];
            secureRandom.nextBytes(keyBytes);
            log.warn("[LocalKms] coder.kms.local.master-key is empty. A random KEK is generated; "
                    + "ciphertexts will not be decryptable after restart.");
        } else {
            keyBytes = Base64.getDecoder().decode(masterKeyBase64);
            if (keyBytes.length != 32) {
                throw new IllegalStateException("coder.kms.local.master-key must be 32 bytes (Base64).");
            }
        }
        this.masterKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * SecureRandom으로 난수를 생성
     * @param numBytes 생성할 바이트 수
     * @return 난수 바이트 배열
     */
    @Override
    public byte[] generateRandom(int numBytes) {
        byte[] bytes = new byte[numBytes];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    /**
     * AES-256-GCM으로 암호화하고 "local:kekId:버전:Base64(iv+암호문)" 형식의 암호문을 만든다
     * @param kekId 암호화에 사용할 KEK 식별자 (AAD로 사용)
     * @param plaintext 암호화할 평문
     * @return 암호문과 KEK 식별자, 감사 ID
     * @throws IllegalStateException 암호화에 실패한 경우
     */
    @Override
    public KmsEncryptResult encrypt(String kekId, byte[] plaintext) {
        try {
            byte[] iv = generateRandom(IV_BYTES);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(kekId.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(plaintext);
            byte[] payload = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
            String ciphertext = String.join(":", "local", kekId, KEK_VERSION, Base64.getEncoder().encodeToString(payload));
            return new KmsEncryptResult(ciphertext, kekId, newAuditId());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("local kms encrypt failed", e);
        }
    }

    /**
     * encrypt가 만든 암호문을 복호화
     * @param ciphertext "local:kekId:버전:Base64(iv+암호문)" 형식의 암호문
     * @return 복호화된 평문
     * @throws IllegalArgumentException 지원하지 않는 암호문 형식인 경우
     * @throws IllegalStateException 복호화에 실패한 경우
     */
    @Override
    public byte[] decrypt(String ciphertext) {
        String[] parts = ciphertext.split(":", 4);
        if (parts.length != 4 || !"local".equals(parts[0])) {
            throw new IllegalArgumentException("unsupported ciphertext format");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(parts[3]);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(TAG_BITS, payload, 0, IV_BYTES));
            cipher.updateAAD(parts[1].getBytes(StandardCharsets.UTF_8));
            return cipher.doFinal(payload, IV_BYTES, payload.length - IV_BYTES);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("local kms decrypt failed", e);
        }
    }

    /**
     * RSA-2048 키쌍을 생성해 서명키의 새 버전으로 메모리에 등록
     * @param signingKeyId 서명키 ID
     * @return 생성된 서명키 버전 정보 (공개키 포함)
     * @throws IllegalStateException 키 생성에 실패한 경우
     */
    @Override
    public SigningKeyVersion createSigningKeyVersion(String signingKeyId) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048, secureRandom);
            KeyPair keyPair = generator.generateKeyPair();

            List<LocalSigningKey> versions = signingKeys.computeIfAbsent(signingKeyId, id -> new CopyOnWriteArrayList<>());
            String version = "v" + (versions.size() + 1);
            String kid = signingKeyId + "-" + version + "-" + UUID.randomUUID().toString().substring(0, 8);
            SigningKeyVersion metadata = new SigningKeyVersion(
                    signingKeyId, version, kid, keyPair.getPublic(), clock.instant(), newAuditId());
            versions.add(new LocalSigningKey(metadata, keyPair.getPrivate()));
            return metadata;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("local kms key generation failed", e);
        }
    }

    /**
     * 서명키의 모든 버전을 생성 시각 최신순으로 반환
     * @param signingKeyId 서명키 ID
     * @return 최신순 서명키 버전 목록 (없으면 빈 목록)
     */
    @Override
    public List<SigningKeyVersion> getSigningKeyVersions(String signingKeyId) {
        List<SigningKeyVersion> versions = new ArrayList<>();
        signingKeys.getOrDefault(signingKeyId, List.of()).forEach(key -> versions.add(key.metadata()));
        versions.sort(Comparator.comparing(SigningKeyVersion::createdAt).reversed());
        return versions;
    }

    /**
     * 지정한 서명키 버전의 개인키로 SHA256withRSA 서명
     * @param signingKeyId 서명키 ID
     * @param version 서명키 버전
     * @param data 서명할 데이터
     * @return 서명값
     * @throws IllegalArgumentException 서명키 버전을 찾을 수 없는 경우
     * @throws IllegalStateException 서명에 실패한 경우
     */
    @Override
    public byte[] sign(String signingKeyId, String version, byte[] data) {
        LocalSigningKey key = signingKeys.getOrDefault(signingKeyId, List.of())
                .stream()
                .filter(candidate -> candidate.metadata().version().equals(version))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("signing key not found: " + signingKeyId + "/" + version));
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key.privateKey());
            signature.update(data);
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("local kms sign failed", e);
        }
    }

    /**
     * 로컬 KMS용 감사 ID를 생성
     * @return "local-" 접두사가 붙은 UUID 감사 ID
     */
    private String newAuditId() {
        return "local-" + UUID.randomUUID();
    }

    /**
     * 서명키 버전 정보와 개인키를 묶어 보관하는 내부 레코드
     */
    private record LocalSigningKey(SigningKeyVersion metadata, PrivateKey privateKey) {
    }
}
