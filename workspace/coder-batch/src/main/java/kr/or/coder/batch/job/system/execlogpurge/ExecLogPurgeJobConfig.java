package kr.or.coder.batch.job.system.execlogpurge;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * [SY] 배치 실행이력 정리 (Tasklet형 샘플)
 * <p>
 * 보관 기간(retentionDays, 기본 coder.batch.retention-days)이 지난 BATCH_* 메타 데이터와 TB_DSPT_EXEC_L을 삭제한다.
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
@Configuration
@RequiredArgsConstructor
public class ExecLogPurgeJobConfig {

    public static final String JOB_NAME = "execLogPurgeJob";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    /**
     * 실행이력 정리 Job을 정의한다.
     * @param execLogPurgeStep 실행이력 정리 Step
     * @return 실행이력 정리 Job
     */
    @Bean
    public Job execLogPurgeJob(Step execLogPurgeStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(execLogPurgeStep)
                .build();
    }

    /**
     * 실행이력 정리 Tasklet을 실행하는 Step을 정의한다.
     * @param execLogPurgeTasklet 실행이력 정리 Tasklet
     * @return 실행이력 정리 Step
     */
    @Bean
    public Step execLogPurgeStep(ExecLogPurgeTasklet execLogPurgeTasklet) {
        return new StepBuilder("execLogPurgeStep", jobRepository)
                .tasklet(execLogPurgeTasklet, transactionManager)
                .build();
    }
}
