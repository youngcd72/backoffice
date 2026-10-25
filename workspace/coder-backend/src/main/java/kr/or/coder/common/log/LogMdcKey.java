package kr.or.coder.common.log;

/**
 * 로그 MDC 키 상수 (logback-spring.xml 패턴·에러 JSON 필드명과 동일)
 * @author ywkim
 * @since 2026.10.09
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.09  ywkim        최초 생성
 * </pre>
 */
public final class LogMdcKey {

    /** 요청 추적 ID (응답 헤더 X-Trace-Id) */
    public static final String TRACE_ID = "traceId";
    /** 로그인 사용자 ID (USR_ID) */
    public static final String USR_ID = "usrId";
    /** 오류가 난 쿼리 ID (SQL_ID, {매퍼명}.{쿼리ID}) */
    public static final String SQL_ID = "sqlId";
    /** 에러 코드 */
    public static final String ERR_CD = "errCd";
    /** 요청 URI */
    public static final String URI = "uri";
    /** HTTP 메서드 */
    public static final String METHOD = "method";

    /** 에러 로그 JSON 필드: 로그일시 (logback timestamp) */
    public static final String LOG_DTTM = "logDttm";
    /** 에러 로그 JSON 필드: 시스템유형 (coder.log.sysm-cls-cd) */
    public static final String SYSM_CLS_CD = "sysmClsCd";
    /** 에러 로그 JSON 필드: 에러내용 (로그 메시지) */
    public static final String ERR_MSG = "errMsg";
    /** 에러 로그 JSON 필드: 로거 */
    public static final String LOGGER = "logger";
    /** 에러 로그 JSON 필드: 스레드 */
    public static final String THREAD = "thread";
    /** 에러 로그 JSON 필드: 스택트레이스 */
    public static final String STACK_TRACE = "stackTrace";

    /** 추적 ID 응답 헤더 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private LogMdcKey() {
    }
}
