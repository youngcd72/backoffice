package kr.or.coder.biz.system.link.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYLK010 연계기관 응답 (TB_LNK_ORG_M)
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
public class LinkOrgResponse {

    /** 연계기관ID */
    private String lnkOrgId;

    /** 연계기관명 */
    private String lnkOrgNm;

    /** 채널구분코드 (공통코드 SY/CHNL_CLS_CD) */
    private String chnlClsCd;

    /** 사용시작일자 (YYYYMMDD) */
    private String useStDt;

    /** 사용종료일자 (YYYYMMDD) */
    private String useEndDt;

    /** 사용여부 */
    private String useYn;

    /** 비고 */
    private String rmk;

    /** 사용 중이면서 만료되지 않은 키 수 (목록 조회에서만 채움) */
    private Integer useKeyCnt;
}
