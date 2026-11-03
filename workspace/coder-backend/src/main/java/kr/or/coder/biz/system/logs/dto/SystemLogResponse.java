package kr.or.coder.biz.system.logs.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYLO030 시스템로그 1건 (에러 로그 파일 JSON 한 줄 + 사용자 정보)
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
@Getter
@Setter
@NoArgsConstructor
public class SystemLogResponse {

    /** 로그 키 ({일자}-{파일순번}-{줄번호}, 상세 조회용) */
    private String logKey;

    /** 로그일시 (yyyy-MM-dd HH:mm:ss.SSS) */
    private String logDttm;

    /** 시스템구분코드 (SY003) */
    private String sysmClsCd;

    /** 사용자ID */
    private String usrId;

    /** 사용자명 (TB_USR_M) */
    private String usrNm;

    /** 사용자구분코드 (TB_USR_M, SY002) */
    private String usrClsCd;

    /** 부서코드 (TB_USR_M) */
    private String deptCd;

    /** 에러내용 */
    private String errMsg;

    /** SQL ID ({매퍼명}.{쿼리ID}) */
    private String sqlId;

    /** 에러코드 */
    private String errCd;

    /** 추적ID */
    private String traceId;

    /** 요청 URI */
    private String uri;

    /** HTTP 메서드 */
    private String method;

    /** 로거 */
    private String logger;

    /** 스레드 */
    private String thread;

    /** 스택트레이스 (상세 조회에서만 채운다) */
    private String stackTrace;
}
