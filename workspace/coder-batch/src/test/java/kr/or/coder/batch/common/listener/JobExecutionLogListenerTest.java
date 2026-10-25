package kr.or.coder.batch.common.listener;

import kr.or.coder.batch.frame.entity.BatchExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JobExecutionLogListener 단위 테스트
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
class JobExecutionLogListenerTest {

    /**
     * Step별 read/write/skip 건수를 합산하고 종료 상태와 오류메시지를 만드는지 검증한다.
     */
    @Test
    @DisplayName("Step별 read/write/skip 건수를 합산하고 종료 상태와 오류메시지를 만든다")
    void summarize() {
        JobExecution jobExecution = new JobExecution(new JobInstance(1L, "sampleJob"), 10L, new JobParameters());
        jobExecution.setStatus(BatchStatus.FAILED);
        LocalDateTime endTime = LocalDateTime.of(2026, 10, 9, 3, 0);
        jobExecution.setEndTime(endTime);

        StepExecution step1 = jobExecution.createStepExecution("step1");
        step1.setReadCount(100);
        step1.setWriteCount(98);
        step1.setReadSkipCount(1);
        step1.setWriteSkipCount(1);

        StepExecution step2 = jobExecution.createStepExecution("step2");
        step2.setReadCount(50);
        step2.setWriteCount(40);
        step2.setProcessSkipCount(2);

        jobExecution.addFailureException(new IllegalStateException("boom"));

        BatchExecution summary = JobExecutionLogListener.summarize(jobExecution);

        assertThat(summary.getJobExecId()).isEqualTo(10L);
        assertThat(summary.getExecStatCd()).isEqualTo("FAILED");
        assertThat(summary.getExecEndDttm()).isEqualTo(endTime);
        assertThat(summary.getProcCnt()).isEqualTo(150);
        assertThat(summary.getSuccCnt()).isEqualTo(138);
        assertThat(summary.getFailCnt()).isEqualTo(4);
        assertThat(summary.getErrMsgCont()).contains("boom");
    }
}
