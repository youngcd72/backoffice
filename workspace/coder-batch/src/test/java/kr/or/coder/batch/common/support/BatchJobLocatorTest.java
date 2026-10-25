package kr.or.coder.batch.common.support;

import kr.or.coder.batch.common.exception.BatchErrorCode;
import kr.or.coder.batch.common.exception.BatchException;
import kr.or.coder.core.common.util.MessageUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * BatchJobLocator 단위 테스트 (실행클래스명으로 Job 빈 조회)
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
class BatchJobLocatorTest {

    private AnnotationConfigApplicationContext context;
    private BatchJobLocator locator;

    /**
     * BatchException 메시지 조회에 필요한 MessageUtil을 초기화한다.
     */
    @BeforeAll
    static void initMessageUtil() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.setUseCodeAsDefaultMessage(true);
        new MessageUtil(messageSource);
    }

    /**
     * Job 1개 / 2개를 정의한 설정 클래스로 컨텍스트를 만든다.
     */
    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(SingleJobConfig.class, DoubleJobConfig.class);
        locator = new BatchJobLocator(context.getBeanFactory());
    }

    /**
     * 테스트 컨텍스트를 닫는다.
     */
    @AfterEach
    void tearDown() {
        context.close();
    }

    /**
     * 설정 클래스 이름으로 그 클래스에 정의된 Job 빈을 찾는지 검증한다.
     */
    @Test
    @DisplayName("실행클래스명으로 그 클래스에 정의된 Job 빈을 찾는다")
    void getJob() {
        Job job = locator.getJob(SingleJobConfig.class.getName());

        assertThat(job).isSameAs(context.getBean("singleJob"));
        assertThat(locator.isRegistered(SingleJobConfig.class.getName())).isTrue();
    }

    /**
     * Job 빈이 없는 클래스는 JOB_BEAN_NOT_REGISTERED 오류인지 검증한다.
     */
    @Test
    @DisplayName("Job 빈이 없는 클래스는 JOB_BEAN_NOT_REGISTERED")
    void getJobNotRegistered() {
        assertThatThrownBy(() -> locator.getJob("kr.or.coder.batch.job.UnknownJobConfig"))
                .isInstanceOf(BatchException.class)
                .extracting("code")
                .isEqualTo(BatchErrorCode.JOB_BEAN_NOT_REGISTERED.getMessageKey());
        assertThat(locator.isRegistered("kr.or.coder.batch.job.UnknownJobConfig")).isFalse();
    }

    /**
     * Job 빈이 2개 정의된 클래스는 JOB_CLASS_AMBIGUOUS 오류인지 검증한다.
     */
    @Test
    @DisplayName("Job 빈이 2개 정의된 클래스는 JOB_CLASS_AMBIGUOUS")
    void getJobAmbiguous() {
        assertThatThrownBy(() -> locator.getJob(DoubleJobConfig.class.getName()))
                .isInstanceOf(BatchException.class)
                .extracting("code")
                .isEqualTo(BatchErrorCode.JOB_CLASS_AMBIGUOUS.getMessageKey());
        assertThat(locator.isRegistered(DoubleJobConfig.class.getName())).isFalse();
    }

    /** Job 1개를 정의한 설정 클래스 */
    @Configuration
    static class SingleJobConfig {

        /**
         * 테스트용 Job
         * @return Job 목
         */
        @Bean
        Job singleJob() {
            return mock(Job.class);
        }
    }

    /** Job 2개를 정의한 설정 클래스 */
    @Configuration
    static class DoubleJobConfig {

        /**
         * 테스트용 Job 1
         * @return Job 목
         */
        @Bean
        Job firstJob() {
            return mock(Job.class);
        }

        /**
         * 테스트용 Job 2
         * @return Job 목
         */
        @Bean
        Job secondJob() {
            return mock(Job.class);
        }
    }
}
