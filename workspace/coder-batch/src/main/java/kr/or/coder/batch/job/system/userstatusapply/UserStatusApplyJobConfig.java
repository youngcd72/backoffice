package kr.or.coder.batch.job.system.userstatusapply;

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
 * [SY] 사용자상태변경 적용 (배치ID BHFL001, 매일 00:10)
 * <p>
 * 사용자상태(USR_STAT_CD)가 퇴직(R) 또는 휴직(L)인 사용자의 사용여부(USE_YN)를 N으로 바꾸고,
 * 퇴직 사용자는 사용종료일자(USE_END_DT)를 퇴사일자(RESI_DT)로 맞춘다.
 * @author ywkim
 * @since 2026.10.17
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.17  ywkim        최초 생성
 * </pre>
 */
@Configuration
@RequiredArgsConstructor
public class UserStatusApplyJobConfig {

    public static final String JOB_NAME = "userStatusApplyJob";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    /**
     * 사용자상태변경 적용 Job을 정의한다.
     * @param userStatusApplyStep 사용자상태변경 적용 Step
     * @return 사용자상태변경 적용 Job
     */
    @Bean
    public Job userStatusApplyJob(Step userStatusApplyStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(userStatusApplyStep)
                .build();
    }

    /**
     * 사용자상태변경 적용 Tasklet을 실행하는 Step을 정의한다.
     * @param userStatusApplyTasklet 사용자상태변경 적용 Tasklet
     * @return 사용자상태변경 적용 Step
     */
    @Bean
    public Step userStatusApplyStep(UserStatusApplyTasklet userStatusApplyTasklet) {
        return new StepBuilder("userStatusApplyStep", jobRepository)
                .tasklet(userStatusApplyTasklet, transactionManager)
                .build();
    }
}
