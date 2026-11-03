package kr.or.coder.biz.system.link.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYLK010 호출이력 조회 조건 (TB_INFC_CALL_L)
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
public class InterfaceCallRequest {

    /** 요청 시작일자 (YYYYMMDD) */
    private String stDt;

    /** 요청 종료일자 (YYYYMMDD) */
    private String endDt;

    /** 연계기관ID */
    private String lnkOrgId;

    /** 인터페이스ID */
    private String infcId;

    /** 결과 (S 성공: HTTP 2xx, F 실패: 그 외) */
    private String rsltCd;

    /** 추적ID (완전 일치) */
    private String trcId;

    /** 최대 조회 건수 (없거나 서버 최대보다 크면 서버 최대) */
    private Integer size;
}
