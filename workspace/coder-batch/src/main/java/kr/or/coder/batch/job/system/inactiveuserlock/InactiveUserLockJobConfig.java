package kr.or.coder.batch.job.system.inactiveuserlock;

import kr.or.coder.batch.common.config.BatchProperties;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.mybatis.spring.batch.builder.MyBatisCursorItemReaderBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * [SY] 장기 미접속 사용자 잠금 (Chunk형 샘플)
 * <p>
 * 최종접속일시(FINA_ACC_DTTM)가 idleDays(기본 90일) 이전인 사용 중 사용자를 잠금 처리한다.
 * 최종접속일시가 없는 사용자(미접속 신규 계정)는 대상에서 제외한다.
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
public class InactiveUserLockJobConfig {

    public static final String JOB_NAME = "inactiveUserLockJob";
    private static final int DEFAULT_IDLE_DAYS = 90;
    private static final String MAPPER_NAMESPACE =
            "kr.or.coder.batch.job.system.inactiveuserlock.mapper.InactiveUserLockBatchMapper";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SqlSessionFactory sqlSessionFactory;
    private final BatchProperties batchProperties;

    /**
     * 장기 미접속 사용자 잠금 Job을 정의한다.
     * @param inactiveUserLockStep 사용자 잠금 Step
     * @return 사용자 잠금 Job
     */
    @Bean
    public Job inactiveUserLockJob(Step inactiveUserLockStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(inactiveUserLockStep)
                .build();
    }

    /**
     * Reader/Processor/Writer로 구성된 Chunk Step을 정의한다. 일시적 DB 오류는 재시도하고 무결성 위반은 건너뛴다.
     * @param inactiveUserReader 장기 미접속 사용자 Reader
     * @param inactiveUserLockProcessor 잠금 정보 설정 Processor
     * @param inactiveUserLockWriter 잠금 갱신 Writer
     * @return 사용자 잠금 Step
     */
    @Bean
    public Step inactiveUserLockStep(MyBatisCursorItemReader<InactiveUser> inactiveUserReader,
                                     InactiveUserLockProcessor inactiveUserLockProcessor,
                                     MyBatisBatchItemWriter<InactiveUser> inactiveUserLockWriter) {
        return new StepBuilder("inactiveUserLockStep", jobRepository)
                .<InactiveUser, InactiveUser>chunk(batchProperties.getDefaultChunkSize(), transactionManager)
                .reader(inactiveUserReader)
                .processor(inactiveUserLockProcessor)
                .writer(inactiveUserLockWriter)
                .faultTolerant()
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .skip(DataIntegrityViolationException.class)
                .skipLimit(10)
                .build();
    }

    /**
     * 장기 미접속 사용자를 Cursor 방식으로 읽는 Reader를 만든다.
     * 처리 중 대상 조건(USR_LOCK_YN)이 바뀌므로 Paging Reader 대신 Cursor Reader를 쓴다
     * (Paging Reader는 OFFSET이 밀려 대상이 누락됨).
     * @param idleDays 미접속 기준 일수 JobParameter, 없으면 기본 90일
     * @return 장기 미접속 사용자 Reader
     */
    @Bean
    @StepScope
    public MyBatisCursorItemReader<InactiveUser> inactiveUserReader(
            @Value("#{jobParameters['idleDays']}") String idleDays) {
        int days = StringUtils.hasText(idleDays) ? Integer.parseInt(idleDays) : DEFAULT_IDLE_DAYS;
        LocalDateTime baseDttm = LocalDate.now().minusDays(days).atStartOfDay();

        return new MyBatisCursorItemReaderBuilder<InactiveUser>()
                .sqlSessionFactory(sqlSessionFactory)
                .queryId(MAPPER_NAMESPACE + ".selectInactiveUserList")
                .parameterValues(Map.of("baseDttm", baseDttm))
                .saveState(true)
                .build();
    }

    /**
     * 사용자 잠금 정보를 일괄 갱신하는 Writer를 만든다.
     * @return 사용자 잠금 Writer
     */
    @Bean
    public MyBatisBatchItemWriter<InactiveUser> inactiveUserLockWriter() {
        return new MyBatisBatchItemWriterBuilder<InactiveUser>()
                .sqlSessionFactory(sqlSessionFactory)
                .statementId(MAPPER_NAMESPACE + ".updateUserLock")
                .assertUpdates(true)
                .build();
    }
}
