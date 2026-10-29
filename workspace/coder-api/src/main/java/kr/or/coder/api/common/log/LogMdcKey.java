package kr.or.coder.api.common.log;

/**
 * 로그 MDC 키 상수 (logback-spring.xml 패턴·에러 JSON 필드명과 동일, coder-backend 와 같은 키)
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
public final class LogMdcKey {

    /** 요청 추적 ID (요청·응답 헤더 X-Trace-Id) */
    public static final String TRACE_ID = "traceId";
    /** 호출자 ID (coder-api 는 연계키ID KEY_ID) */
    public static final String USR_ID = "usrId";
    /** 오류가 난 쿼리 ID (SQL_ID, {매퍼명}.{쿼리ID}) */
    public static final String SQL_ID = "sqlId";
    /** 에러 코드 */
    public static final String ERR_CD = "errCd";
    /** 요청 URI */
    public static final String URI = "uri";
    /** HTTP 메서드 */
    public static final String METHOD = "method";

    /** 추적 ID 요청·응답 헤더 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private LogMdcKey() {
    }
}
