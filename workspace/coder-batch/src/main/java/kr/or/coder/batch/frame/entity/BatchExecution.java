package kr.or.coder.batch.frame.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * TB_DSPT_EXEC_L (배치실행내역) Entity
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
public class BatchExecution {

    /** 배치실행일련번호 */
    private Long dsptExecSn;
    /** 직무ID */
    private String jobId;
    /** 직무실행ID (BATCH_JOB_EXECUTION.JOB_EXECUTION_ID) */
    private Long jobExecId;
    /** 실행구분코드 (S/M/R) */
    private String execClsCd;
    /** 실행상태코드 */
    private String execStatCd;
    private LocalDateTime execStDttm;
    private LocalDateTime execEndDttm;
    /** 처리건수 (Σ readCount) */
    private long procCnt;
    /** 성공건수 (Σ writeCount) */
    private long succCnt;
    /** 실패건수 (Σ skipCount) */
    private long failCnt;
    /** 오류메시지내용 */
    private String errMsgCont;
    /** 실행서버명 (Quartz instanceId) */
    private String execSrvNm;
    private String firsRegrId;
    private String finaRegrId;
}
