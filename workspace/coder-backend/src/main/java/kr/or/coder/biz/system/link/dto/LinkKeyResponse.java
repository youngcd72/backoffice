package kr.or.coder.biz.system.link.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYLK010 클라이언트 키 응답 (TB_LNK_KEY_M, 암호화 비밀키는 넣지 않음)
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
@Getter
@Setter
@NoArgsConstructor
public class LinkKeyResponse {

    /** 키ID (OAuth2 client_id) */
    private String keyId;

    /** 연계기관ID */
    private String lnkOrgId;

    /** 키상태코드 (공통코드 SY/KEY_STAT_CD: 01 사용, 02 사용중지, 03 폐기) */
    private String keyStatCd;

    /** 발급일시 (YYYY-MM-DDTHH:mm:ss.SSS) */
    private String issuDttm;

    /** 만료일시 (YYYY-MM-DDTHH:mm:ss.SSS, 없으면 만료 없음) */
    private String expiDttm;

    /** 허용IP주소 (IP/CIDR 쉼표 구분, 없으면 제한 없음) */
    private String allwIpAddr;

    /** 분호출한도건수 (없으면 제한 없음) */
    private Integer miCallLmtCnt;

    /** 일호출한도건수 (없으면 제한 없음) */
    private Integer dayCallLmtCnt;

    /** 동시요청한도건수 (없으면 제한 없음) */
    private Integer simuRqmtLmtCnt;

    /** 비고 */
    private String rmk;
}
