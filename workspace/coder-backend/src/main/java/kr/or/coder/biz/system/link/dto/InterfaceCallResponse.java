package kr.or.coder.biz.system.link.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYLK010 호출이력 응답 (TB_INFC_CALL_L, 본문은 상세 조회에서만 채움)
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
public class InterfaceCallResponse {

    /** 호출일련번호 */
    private Long callSn;

    /** 추적ID */
    private String trcId;

    /** 키ID */
    private String keyId;

    /** 연계기관ID */
    private String lnkOrgId;

    /** 연계기관명 */
    private String lnkOrgNm;

    /** 인터페이스ID */
    private String infcId;

    /** 인터페이스명 */
    private String infcNm;

    /** HTTP메서드명 */
    private String httpMethNm;

    /** 요청 URL */
    private String url;

    /** HTTP상태코드 */
    private String httpStatCd;

    /** 클라이언트IP주소 */
    private String clntIpAddr;

    /** 요청일시 (YYYY-MM-DDTHH:mm:ss.SSS) */
    private String rqmtDttm;

    /** 응답일시 (YYYY-MM-DDTHH:mm:ss.SSS) */
    private String rspnDttm;

    /** 소요시간 (밀리초) */
    private Long elapsedMs;

    /** 오류코드 */
    private String errCd;

    /** 요청본문 (마스킹된 값) */
    private String rqmtCont;

    /** 응답본문 (마스킹된 값) */
    private String rspnCont;
}
