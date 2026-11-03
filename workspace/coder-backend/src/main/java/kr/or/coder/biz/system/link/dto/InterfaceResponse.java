package kr.or.coder.biz.system.link.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYLK010 인터페이스 응답 (TB_INFC_M)
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
public class InterfaceResponse {

    /** 인터페이스ID (IF + 업무약어 2자리 + 순번 4자리) */
    private String infcId;

    /** 인터페이스명 */
    private String infcNm;

    /** HTTP메서드명 */
    private String httpMethNm;

    /** URL */
    private String url;

    /** 본문로그여부 */
    private String bodyLogYn;

    /** 마스킹필드내용 (쉼표 구분) */
    private String maskFldCont;

    /** 사용여부 */
    private String useYn;

    /** 비고 */
    private String rmk;

    /** 이 인터페이스를 허용한 연계기관 수 */
    private Integer allwOrgCnt;
}
