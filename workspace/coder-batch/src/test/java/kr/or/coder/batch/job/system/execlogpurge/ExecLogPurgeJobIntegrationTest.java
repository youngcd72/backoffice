package kr.or.coder.batch.job.system.execlogpurge;

import kr.or.coder.batch.common.support.BatchConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * execLogPurgeJob 통합 테스트.
 * PostgreSQL에 schema/batch/*.sql, schema/table/TB_DSPT_*.sql 반영 후 실행한다.
 * <pre>./gradlew integrationTest</pre>
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
@Tag("integration")
@SpringBootTest
class ExecLogPurgeJobIntegrationTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier(ExecLogPurgeJobConfig.JOB_NAME)
    private Job execLogPurgeJob;

    /**
     * execLogPurgeJob이 정상 종료(COMPLETED)되는지 검증한다.
     * @throws Exception Job 실행 중 오류가 발생한 경우
     */
    @Test
    @DisplayName("execLogPurgeJob이 정상 종료된다")
    void runExecLogPurgeJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString(BatchConstants.PARM_REQUEST_DTTM, String.valueOf(System.nanoTime()))
                .addString("retentionDays", "36500")
                .addString(BatchConstants.PARM_EXEC_CLS_CD, BatchConstants.EXEC_CLS_MANUAL, false)
                .addString(BatchConstants.PARM_REQ_USR_ID, "TEST", false)
                .toJobParameters();

        JobExecution jobExecution = jobLauncher.run(execLogPurgeJob, params);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
