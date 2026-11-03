package kr.or.coder.biz.system.link.service;

import kr.or.coder.biz.system.link.dto.LinkKeyHistResponse;
import kr.or.coder.biz.system.link.dto.LinkKeyIssueRequest;
import kr.or.coder.biz.system.link.dto.LinkKeyIssueResponse;
import kr.or.coder.biz.system.link.dto.LinkKeyResponse;
import kr.or.coder.biz.system.link.dto.LinkKeySaveRequest;
import kr.or.coder.biz.system.link.mapper.LinkKeyMapper;
import kr.or.coder.biz.system.link.mapper.LinkOrgMapper;
import kr.or.coder.common.util.RowSaveUtil;
import kr.or.coder.core.common.exception.BusinessException;
import kr.or.coder.core.kms.KmsClient;
import kr.or.coder.core.kms.KmsEncryptResult;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * SYLK010 클라이언트 키 Service 구현 (TB_LNK_KEY_M, TB_LNK_KEY_H)
 * 키 발급 방식은 coder-api ClientKeyIssueServiceImpl 과 같다 (키ID ck + 난수 24자리, 비밀키 32바이트, KMS 암호화).
 * @author ywkim
 * @since 2026.11.03
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.11.03  ywkim        최초 생성
 * </pre>
 */
@Service
public class LinkKeyServiceImpl implements LinkKeyService {

    private static final String KEY_ID_PREFIX = "ck";
    private static final int KEY_ID_RANDOM_BYTES = 12;
    private static final int SECRET_BYTES = 32;
    private static final String KEY_STAT_USE = "01";
    private static final String KEY_STAT_REVOKED = "03";
    private static final Map<String, String> KEY_STAT_NM = Map.of("01", "사용", "02", "사용중지", "03", "폐기");
    private static final DateTimeFormatter ISO_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String OCTET = "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)";
    private static final Pattern IPV4_CIDR = Pattern.compile("^" + OCTET + "(\\." + OCTET + "){3}(/(3[0-2]|[12]?\\d))?$");
    private static final Pattern IPV6_CIDR = Pattern.compile("^(?=.*:)[0-9A-Fa-f:]+(/(12[0-8]|1[01]\\d|[1-9]?\\d))?$");

    private final LinkKeyMapper linkKeyMapper;
    private final LinkOrgMapper linkOrgMapper;
    private final ObjectProvider<KmsClient> kmsClientProvider;
    private final LinkCacheEvictPublisher linkCacheEvictPublisher;
    private final String kekId;

    /**
     * 매퍼, KMS, 설정으로 생성
     * @param linkKeyMapper 클라이언트 키 매퍼
     * @param linkOrgMapper 연계기관 매퍼
     * @param kmsClientProvider KMS 클라이언트 (coder.kms.enabled=false 면 없음)
     * @param linkCacheEvictPublisher coder-api 캐시 삭제 알림
     * @param kekId 비밀키 암호화 KEK ID (coder.link.client-secret-kek-id, coder-api 설정과 같아야 함)
     */
    public LinkKeyServiceImpl(LinkKeyMapper linkKeyMapper, LinkOrgMapper linkOrgMapper,
                              ObjectProvider<KmsClient> kmsClientProvider, LinkCacheEvictPublisher linkCacheEvictPublisher,
                              @Value("${coder.link.client-secret-kek-id:api-client-secret}") String kekId) {
        this.linkKeyMapper = linkKeyMapper;
        this.linkOrgMapper = linkOrgMapper;
        this.kmsClientProvider = kmsClientProvider;
        this.linkCacheEvictPublisher = linkCacheEvictPublisher;
        this.kekId = kekId;
    }

    /**
     * 연계기관의 클라이언트 키 목록을 상태·발급일시 최근 순으로 조회 (암호화 비밀키 제외)
     * @param lnkOrgId 연계기관ID
     * @return 클라이언트 키 목록
     * @throws BusinessException 연계기관이 없는 경우 (sy.link.org.not.found)
     */
    @Override
    public List<LinkKeyResponse> getLinkKeyList(String lnkOrgId) {
        checkLinkOrgExists(lnkOrgId);
        return linkKeyMapper.selectLinkKeyList(lnkOrgId);
    }

    /**
     * 클라이언트 키를 발급한다. 비밀키는 KMS 로 암호화해 저장하고 원문은 결과로 한 번만 반환한다 (로그에 남기지 않음).
     * @param lnkOrgId 연계기관ID
     * @param request 유효기간 (일, 0이면 만료 없음)
     * @param usrId 발급 사용자ID
     * @return 키ID, 비밀키 원문, 만료일시
     * @throws BusinessException 연계기관이 없거나 (sy.link.org.not.found), KMS 를 쓸 수 없는 경우 (sy.link.kms.not.configured)
     */
    @Override
    @Transactional
    public LinkKeyIssueResponse saveLinkKey(String lnkOrgId, LinkKeyIssueRequest request, String usrId) {
        checkLinkOrgExists(lnkOrgId);
        KmsClient kmsClient = kmsClientProvider.getIfAvailable();
        if (kmsClient == null) {
            throw new BusinessException("sy.link.kms.not.configured");
        }

        String keyId = KEY_ID_PREFIX + HexFormat.of().formatHex(kmsClient.generateRandom(KEY_ID_RANDOM_BYTES));
        String clientSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(kmsClient.generateRandom(SECRET_BYTES));
        KmsEncryptResult encrypted = kmsClient.encrypt(kekId, clientSecret.getBytes(StandardCharsets.UTF_8));

        LocalDateTime issuDttm = LocalDateTime.now();
        LocalDateTime expiDttm = request.getExpireDays() > 0 ? issuDttm.plusDays(request.getExpireDays()) : null;
        linkKeyMapper.insertLinkKey(keyId, lnkOrgId, encrypted.ciphertext(), encrypted.kekId(), issuDttm, expiDttm, usrId);
        linkKeyMapper.insertLinkKeyHist(keyId, KEY_STAT_USE, "키 발급", encrypted.auditId(), usrId);
        return new LinkKeyIssueResponse(keyId, clientSecret, expiDttm == null ? null : expiDttm.format(ISO_MILLIS));
    }

    /**
     * 클라이언트 키의 상태·만료일·허용IP·호출 한도·비고를 수정하고, 바뀐 항목을 키 이력에 남긴 뒤 coder-api 캐시 삭제를 알린다
     * @param lnkOrgId 연계기관ID
     * @param keyId 키ID
     * @param request 수정 내용
     * @param usrId 수정 사용자ID
     * @throws BusinessException 키가 없거나 (sy.link.key.not.found), 폐기된 키이거나 (sy.link.key.revoked),
     *                           만료일을 오늘 이전으로 바꾸거나, 허용IP 형식이 틀린 경우
     */
    @Override
    @Transactional
    public void saveLinkKeyDtl(String lnkOrgId, String keyId, LinkKeySaveRequest request, String usrId) {
        LinkKeyResponse key = getLinkKey(lnkOrgId, keyId);
        if (KEY_STAT_REVOKED.equals(key.getKeyStatCd())) {
            throw new BusinessException("sy.link.key.revoked");
        }

        String expiDt = StringUtils.hasText(request.getExpiDt()) ? request.getExpiDt() : null;
        String curExpiDt = key.getExpiDttm() == null ? null : key.getExpiDttm().substring(0, 10).replace("-", "");
        boolean expiChanged = !Objects.equals(expiDt, curExpiDt);
        // 만료일을 바꿀 때만 검사한다 (이미 만료된 키의 상태만 바꾸는 경우는 허용)
        if (expiChanged && expiDt != null && LocalDate.parse(expiDt, YMD).isBefore(LocalDate.now())) {
            throw new BusinessException("sy.link.validate.expire.date");
        }
        request.setAllwIpAddr(normalizeIpList(request.getAllwIpAddr()));

        List<String> changes = new ArrayList<>();
        if (!key.getKeyStatCd().equals(request.getKeyStatCd())) {
            changes.add("상태 변경: " + KEY_STAT_NM.get(key.getKeyStatCd()) + " → " + KEY_STAT_NM.get(request.getKeyStatCd()));
        }
        if (expiChanged) {
            changes.add("만료일 변경");
        }
        if (!Objects.equals(emptyToNull(key.getAllwIpAddr()), emptyToNull(request.getAllwIpAddr()))) {
            changes.add("허용IP 변경");
        }
        if (!Objects.equals(key.getMiCallLmtCnt(), request.getMiCallLmtCnt())) {
            changes.add("분당 호출 한도 변경");
        }
        if (!Objects.equals(key.getDayCallLmtCnt(), request.getDayCallLmtCnt())) {
            changes.add("일 호출 한도 변경");
        }
        if (!Objects.equals(key.getSimuRqmtLmtCnt(), request.getSimuRqmtLmtCnt())) {
            changes.add("동시 요청 한도 변경");
        }
        if (!Objects.equals(emptyToNull(key.getRmk()), emptyToNull(request.getRmk()))) {
            changes.add("비고 변경");
        }

        RowSaveUtil.checkUpdated(linkKeyMapper.updateLinkKey(lnkOrgId, keyId, request, usrId));
        if (!changes.isEmpty()) {
            linkKeyMapper.insertLinkKeyHist(keyId, request.getKeyStatCd(), String.join("; ", changes), null, usrId);
        }
        linkCacheEvictPublisher.publish("KEY:" + keyId);
    }

    /**
     * 클라이언트 키 이력을 최근 순으로 조회
     * @param lnkOrgId 연계기관ID
     * @param keyId 키ID
     * @return 키 이력 목록
     * @throws BusinessException 연계기관에 키가 없는 경우 (sy.link.key.not.found)
     */
    @Override
    public List<LinkKeyHistResponse> getLinkKeyHistList(String lnkOrgId, String keyId) {
        getLinkKey(lnkOrgId, keyId);
        return linkKeyMapper.selectLinkKeyHistList(keyId);
    }

    /**
     * 연계기관의 클라이언트 키 1건을 조회
     * @param lnkOrgId 연계기관ID
     * @param keyId 키ID
     * @return 클라이언트 키
     * @throws BusinessException 키가 없는 경우 (sy.link.key.not.found)
     */
    private LinkKeyResponse getLinkKey(String lnkOrgId, String keyId) {
        LinkKeyResponse key = linkKeyMapper.selectLinkKey(lnkOrgId, keyId);
        if (key == null) {
            throw new BusinessException("sy.link.key.not.found", keyId);
        }
        return key;
    }

    /**
     * 허용IP 목록의 형식을 검사하고 공백을 정리해 쉼표 구분 문자열로 만든다
     * @param allwIpAddr 허용IP (쉼표 구분)
     * @return 정리한 허용IP (비어 있으면 null)
     * @throws BusinessException IP 또는 CIDR 형식이 아닌 항목이 있는 경우 (sy.link.validate.ip)
     */
    private String normalizeIpList(String allwIpAddr) {
        if (!StringUtils.hasText(allwIpAddr)) {
            return null;
        }
        List<String> ipList = Arrays.stream(allwIpAddr.split(",")).map(String::trim).filter(StringUtils::hasText).toList();
        for (String ip : ipList) {
            if (!IPV4_CIDR.matcher(ip).matches() && !IPV6_CIDR.matcher(ip).matches()) {
                throw new BusinessException("sy.link.validate.ip", ip);
            }
        }
        return ipList.isEmpty() ? null : String.join(",", ipList);
    }

    /**
     * 빈 문자열을 null 로 바꾼다 (변경 여부 비교용)
     * @param value 문자열
     * @return 비어 있으면 null, 아니면 그대로
     */
    private static String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    /**
     * 연계기관이 있는지 확인
     * @param lnkOrgId 연계기관ID
     * @throws BusinessException 연계기관이 없는 경우 (sy.link.org.not.found)
     */
    private void checkLinkOrgExists(String lnkOrgId) {
        if (!linkOrgMapper.existsLinkOrg(lnkOrgId)) {
            throw new BusinessException("sy.link.org.not.found", lnkOrgId);
        }
    }
}
