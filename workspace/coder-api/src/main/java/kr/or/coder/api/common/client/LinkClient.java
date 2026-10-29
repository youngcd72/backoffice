package kr.or.coder.api.common.client;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 연계키와 연계기관 정보 (TB_LNK_KEY_M + TB_LNK_ORG_M)
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
@Getter
@Setter
public class LinkClient {

    /** 키상태코드: 사용 */
    public static final String KEY_STAT_USE = "01";

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;

    private String keyId;
    private String lnkOrgId;
    private String lnkOrgNm;
    private String chnlClsCd;
    private String encKeyVal;
    private String kmsKeyId;
    private String keyStatCd;
    private LocalDateTime issuDttm;
    private LocalDateTime expiDttm;
    private String allwIpAddr;
    private Integer miCallLmtCnt;
    private Integer dayCallLmtCnt;
    private Integer simuRqmtLmtCnt;
    private String orgUseYn;
    private String useStDt;
    private String useEndDt;

    /**
     * 키와 연계기관을 지금 사용할 수 있는지 확인
     * (키 상태 사용, 만료 전, 연계기관 사용 중, 연계기관 사용기간 안)
     * @param now 기준 일시
     * @return 사용할 수 있으면 true
     */
    public boolean isUsable(LocalDateTime now) {
        String today = now.format(YYYYMMDD);
        return KEY_STAT_USE.equals(keyStatCd)
                && (expiDttm == null || now.isBefore(expiDttm))
                && "Y".equals(orgUseYn)
                && (useStDt == null || useStDt.compareTo(today) <= 0)
                && (useEndDt == null || useEndDt.compareTo(today) >= 0);
    }
}
