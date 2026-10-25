package kr.or.coder.batch.frame.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * TB_DSPT_JOB_M (배치직무기본) Entity
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
public class BatchJob {

    /** 직무ID (배치ID, 예: BHFL001) */
    private String jobId;
    /** 직무명 */
    private String jobNm;
    /** 실행클래스명 (Job 빈을 정의한 클래스, 패키지 포함) */
    private String execClassNm;
    /** 업무모듈코드 (Quartz JobKey/TriggerKey group) */
    private String wrkMdulCd;
    /** 스케줄표현식 (Quartz cron) */
    private String scheExpr;
    /** 미스파이어정책코드 (01 건너뜀, 02 즉시 1회 실행) */
    private String msfrPlcyCd;
    /** 다중실행여부 */
    private String mltExecYn;
    /** 직무파라미터내용 (JSON) */
    private String jobParmCont;
    /** 사용여부 */
    private String useYn;
    /** 비고 */
    private String rmk;
    /** 반영프로그램ID */
    private String reflPrgmId;
    private String firsRegrId;
    private LocalDateTime firsRegDt;
    private String finaRegrId;
    private LocalDateTime finaRegDt;

    /** 최종 실행내역 (TB_DSPT_EXEC_L 최근 1건, 목록/상세 조회 시에만 채움): 배치실행일련번호 */
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
}
