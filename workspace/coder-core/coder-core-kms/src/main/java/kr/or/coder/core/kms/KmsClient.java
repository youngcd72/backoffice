package kr.or.coder.core.kms;

import java.util.List;

/**
 * KMS 추상화. 업무 코드는 KMS 제품 SDK를 직접 호출하지 않고 이 인터페이스에만 의존한다.
 * 구현체는 coder.kms.provider로 선택한다 (local / vault / aws ...).
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
public interface KmsClient {

    /**
     * 암호학적으로 안전한 난수를 생성한다 (비밀키 생성 등)
     * @param numBytes 생성할 바이트 수
     * @return 난수 바이트 배열
     */
    byte[] generateRandom(int numBytes);

    /**
     * KEK로 평문을 암호화한다. 반환 ciphertext에는 KEK 버전 정보가 포함되어 KEK 회전 후에도 복호화할 수 있다.
     * @param kekId 암호화에 사용할 KEK 식별자
     * @param plaintext 암호화할 평문
     * @return 암호문과 KEK 식별자, 감사 ID
     */
    KmsEncryptResult encrypt(String kekId, byte[] plaintext);

    /**
     * encrypt가 만든 ciphertext를 복호화한다.
     * @param ciphertext 암호문
     * @return 복호화된 평문
     */
    byte[] decrypt(String ciphertext);

    /**
     * 비대칭 서명키의 새 버전을 만든다 (서명키 회전). 개인키는 KMS 밖으로 나오지 않는다.
     * @param signingKeyId 서명키 ID
     * @return 생성된 서명키 버전 정보
     */
    SigningKeyVersion createSigningKeyVersion(String signingKeyId);

    /**
     * 서명키의 모든 버전(공개키 포함)을 최신순으로 반환한다.
     * @param signingKeyId 서명키 ID
     * @return 최신순 서명키 버전 목록
     */
    List<SigningKeyVersion> getSigningKeyVersions(String signingKeyId);

    /**
     * 지정한 서명키 버전으로 서명한다 (SHA256withRSA).
     * @param signingKeyId 서명키 ID
     * @param version 서명키 버전
     * @param data 서명할 데이터
     * @return 서명값
     */
    byte[] sign(String signingKeyId, String version, byte[] data);
}
