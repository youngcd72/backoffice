package kr.or.coder.biz.system.link.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYLK010 기관별 허용 인터페이스 응답 (전체 인터페이스 + 허용 여부, TB_INFC_M + TB_LNK_ORG_INFC_R)
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
public class LinkOrgInterfaceResponse {

    /** 인터페이스ID */
    private String infcId;

    /** 인터페이스명 */
    private String infcNm;

    /** HTTP메서드명 */
    private String httpMethNm;

    /** URL */
    private String url;

    /** 인터페이스 사용여부 */
    private String infcUseYn;

    /** 이 기관 허용 여부 (TB_LNK_ORG_INFC_R.USE_YN, 관계가 없으면 N) */
    private String allwYn;
}
