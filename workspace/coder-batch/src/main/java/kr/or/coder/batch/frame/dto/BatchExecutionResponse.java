package kr.or.coder.batch.frame.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 배치 실행 이력 응답 DTO (목록/상세 공용)
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchExecutionResponse {

    private Long dsptExecSn;
    private String jobId;
    private String jobNm;
    private String wrkMdulCd;
    private Long jobExecId;
    private String execClsCd;
    private String execStatCd;
    private LocalDateTime execStDttm;
    private LocalDateTime execEndDttm;
    private long procCnt;
    private long succCnt;
    private long failCnt;
    private String errMsgCont;
    private String execSrvNm;
    private String firsRegrId;

    /** 상세 조회 시에만 채움: JobParameters */
    private Map<String, String> jobParams;
    /** 상세 조회 시에만 채움: Step별 처리 현황 */
    private List<BatchStepExecutionResponse> steps;
}
