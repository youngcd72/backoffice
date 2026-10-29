package kr.or.coder.api.common.calllog;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 인터페이스 호출 이력 한 건 (TB_INFC_CALL_L)
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
public class CallLog {

    private String trcId;
    private String keyId;
    private String lnkOrgId;
    private String infcId;
    private String httpMethNm;
    private String url;
    private String httpStatCd;
    private String clntIpAddr;
    private LocalDateTime rqmtDttm;
    private LocalDateTime rspnDttm;
    private long elapsedMs;
    private String errCd;
    private String rqmtCont;
    private String rspnCont;
    private String regrId;
}
