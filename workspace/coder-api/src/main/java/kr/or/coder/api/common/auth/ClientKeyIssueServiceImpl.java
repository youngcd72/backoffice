package kr.or.coder.api.common.auth;

import kr.or.coder.api.common.auth.dto.ClientKeyIssueResult;
import kr.or.coder.api.common.auth.mapper.ClientKeyMapper;
import kr.or.coder.api.common.client.LinkClient;
import kr.or.coder.api.common.config.ApiProperties;
import kr.or.coder.core.common.exception.BusinessException;
import kr.or.coder.core.kms.KmsClient;
import kr.or.coder.core.kms.KmsEncryptResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 클라이언트 키 발급 Service 구현.
 * 키ID는 "ck" + 난수 24자리(hex), 비밀키는 32바이트 난수(Base64URL)로 KMS(generateRandom)에서 만든다.
 * @author ywkim
 * @since 2026.10.27
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.27  ywkim        최초 생성
 * </pre>
 */
@Service
public class ClientKeyIssueServiceImpl implements ClientKeyIssueService {

    private static final String KEY_ID_PREFIX = "ck";
    private static final int KEY_ID_RANDOM_BYTES = 12;
    private static final int SECRET_BYTES = 32;

    private final ClientKeyMapper clientKeyMapper;
    private final KmsClient kmsClient;
    private final String kekId;
    private final Clock clock;

    /**
     * 매퍼와 KMS, 설정으로 생성
     * @param clientKeyMapper 클라이언트 키 매퍼
     * @param kmsClient KMS 클라이언트
     * @param apiProperties 대외 연계 API 설정
     * @param clock 시간 기준 Clock
     */
    public ClientKeyIssueServiceImpl(ClientKeyMapper clientKeyMapper, KmsClient kmsClient, ApiProperties apiProperties,
                                     Clock clock) {
        this.clientKeyMapper = clientKeyMapper;
        this.kmsClient = kmsClient;
        this.kekId = apiProperties.getToken().getClientSecretKekId();
        this.clock = clock;
    }

    /**
     * 연계기관에 클라이언트 키를 발급한다. 비밀키는 KMS 로 암호화해 저장하고 원문은 결과로 한 번만 반환한다.
     * @param lnkOrgId 연계기관ID
     * @param expireDays 유효 기간 (일, 0 이하면 만료 없음)
     * @param regrId 등록자ID
     * @return 키ID, 비밀키 원문, 만료일시
     * @throws BusinessException 연계기관이 없는 경우 (sy.api.link.org.not.found)
     */
    @Override
    @Transactional
    public ClientKeyIssueResult issueClientKey(String lnkOrgId, int expireDays, String regrId) {
        if (clientKeyMapper.selectLinkOrgCount(lnkOrgId) == 0) {
            throw new BusinessException("sy.api.link.org.not.found", lnkOrgId);
        }

        String keyId = KEY_ID_PREFIX + HexFormat.of().formatHex(kmsClient.generateRandom(KEY_ID_RANDOM_BYTES));
        String clientSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(kmsClient.generateRandom(SECRET_BYTES));
        KmsEncryptResult encrypted = kmsClient.encrypt(kekId, clientSecret.getBytes(StandardCharsets.UTF_8));

        LocalDateTime issuDttm = LocalDateTime.now(clock);
        LocalDateTime expiDttm = expireDays > 0 ? issuDttm.plusDays(expireDays) : null;
        clientKeyMapper.insertClientKey(keyId, lnkOrgId, encrypted.ciphertext(), encrypted.kekId(), issuDttm, expiDttm, regrId);
        clientKeyMapper.insertClientKeyHist(keyId, LinkClient.KEY_STAT_USE, "키 발급", encrypted.auditId(), regrId);
        return new ClientKeyIssueResult(keyId, clientSecret, expiDttm);
    }
}
