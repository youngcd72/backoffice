package kr.or.coder.batch.frame.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 배치직무 응답 DTO (직무 정보와 Quartz 스케줄 상태 포함)
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
public class BatchJobResponse {

    private String jobId;
    private String jobNm;
    /** 실행클래스명 (Job 빈을 정의한 클래스, 패키지 포함) */
    private String execClassNm;
    private String wrkMdulCd;
    private String scheExpr;
    private String msfrPlcyCd;
    private String mltExecYn;
    private String jobParmCont;
    private String useYn;
    private String rmk;
    private String reflPrgmId;
    private String firsRegrId;
    private LocalDateTime firsRegDt;
    private String finaRegrId;
    private LocalDateTime finaRegDt;

    /** Spring Batch Job 빈 등록 여부 (Y/N) */
    private String jobBeanYn;
    /** Quartz Trigger 상태 (NORMAL, PAUSED, BLOCKED, ERROR, COMPLETE, NONE) */
    private String schedStatus;
    /** 다음 실행 예정 일시 */
    private LocalDateTime nextFireDttm;
    /** 이전 실행 일시 */
    private LocalDateTime prevFireDttm;

    /** 최종 실행내역 (TB_DSPT_EXEC_L 최근 1건): 배치실행일련번호 */
    private Long lastDsptExecSn;
    /** 최종 실행상태코드 */
    private String lastExecStatCd;
    /** 최종 실행시작일시 */
    private LocalDateTime lastExecStDttm;
    /** 최종 실행종료일시 */
    private LocalDateTime lastExecEndDttm;
    /** 최종 처리건수 */
    private Long lastProcCnt;
    /** 최종 성공건수 */
    private Long lastSuccCnt;
    /** 최종 실패건수 */
    private Long lastFailCnt;
    /** 최종 오류메시지내용 */
    private String lastErrMsgCont;

    /** 실패 알람 수신자 목록 (상세 조회에서만 채움) */
    private List<BatchAlarmRcvrResponse> alrmRcvrList;
}
