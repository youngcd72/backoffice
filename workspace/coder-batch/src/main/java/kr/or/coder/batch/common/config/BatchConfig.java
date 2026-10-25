package kr.or.coder.batch.common.config;

import kr.or.coder.batch.common.listener.JobExecutionLogListener;
import kr.or.coder.batch.common.listener.StepMdcListener;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 배치 공통 설정 (공통 Listener 등록, BatchProperties 활성화)
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
@EnableConfigurationProperties(BatchProperties.class)
public class BatchConfig {

    /*
     * Job 빈의 JobRegistry 등록은 Spring Batch 기본 구성(DefaultBatchConfiguration)이 처리한다.
     * JobRegistryBeanPostProcessor 등을 추가로 선언하면 DuplicateJobException이 발생한다.
     */

    /**
     * 모든 Job/Step에 공통 Listener를 등록한다.
     * 각 JobConfig에서 listener를 빠뜨려도 실행내역(TB_DSPT_EXEC_L)과 MDC 로깅이 누락되지 않도록 한다.
     * @param applicationContext Job/Step 빈을 조회할 ApplicationContext
     * @param jobExecutionLogListener 실행내역 기록용 Job Listener
     * @param stepMdcListener Step명 MDC 설정용 Step Listener
     * @return 모든 싱글톤 초기화 후 Listener를 등록하는 콜백
     */
    @Bean
    public SmartInitializingSingleton commonListenerRegistrar(ApplicationContext applicationContext,
                                                              JobExecutionLogListener jobExecutionLogListener,
                                                              StepMdcListener stepMdcListener) {
        return () -> {
            applicationContext.getBeansOfType(AbstractJob.class)
                    .values()
                    .forEach(job -> job.registerJobExecutionListener(jobExecutionLogListener));
            applicationContext.getBeansOfType(AbstractStep.class)
                    .values()
                    .forEach(step -> step.registerStepExecutionListener(stepMdcListener));
        };
    }
}
