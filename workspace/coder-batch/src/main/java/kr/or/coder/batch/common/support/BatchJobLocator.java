package kr.or.coder.batch.common.support;

import kr.or.coder.batch.common.exception.BatchErrorCode;
import kr.or.coder.batch.common.exception.BatchException;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 실행클래스명(EXEC_CLASS_NM)으로 Spring Batch Job 빈을 찾는 컴포넌트.
 * 실행클래스명은 Job 빈을 정의한 클래스(보통 {기능}JobConfig)의 패키지 포함 전체 이름이며,
 * 한 클래스에 Job 빈은 1개만 정의해야 한다.
 * @author ywkim
 * @since 2026.10.17
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.17  ywkim        최초 생성 (배치ID와 Job 빈 이름 분리)
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class BatchJobLocator {

    private final ConfigurableListableBeanFactory beanFactory;

    /**
     * 실행클래스명에 해당하는 Job 빈을 찾는다.
     * @param execClassNm 실행클래스명 (패키지 포함)
     * @return Job 빈
     * @throws BatchException 해당 클래스에 Job 빈이 없거나 2개 이상인 경우
     */
    public Job getJob(String execClassNm) {
        List<String> beanNames = getJobBeanNamesByClass().getOrDefault(execClassNm, List.of());
        if (beanNames.isEmpty()) {
            throw new BatchException(BatchErrorCode.JOB_BEAN_NOT_REGISTERED, execClassNm);
        }
        if (beanNames.size() > 1) {
            throw new BatchException(BatchErrorCode.JOB_CLASS_AMBIGUOUS, execClassNm, beanNames.size());
        }
        return beanFactory.getBean(beanNames.get(0), Job.class);
    }

    /**
     * 실행클래스명에 Job 빈이 정확히 1개 정의되어 있는지 판별한다.
     * @param execClassNm 실행클래스명 (패키지 포함)
     * @return Job 빈이 1개 있으면 true
     */
    public boolean isRegistered(String execClassNm) {
        return StringUtils.hasText(execClassNm)
                && getJobBeanNamesByClass().getOrDefault(execClassNm, List.of()).size() == 1;
    }

    /**
     * 등록된 Job 빈 이름을 정의 클래스명별로 모은다.
     * {@code @Bean} 메서드로 만든 Job은 그 메서드가 있는 설정 클래스, 클래스로 등록한 Job은 그 클래스 기준이다.
     * @return 정의 클래스명(패키지 포함) → Job 빈 이름 목록
     */
    public Map<String, List<String>> getJobBeanNamesByClass() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String beanName : beanFactory.getBeanNamesForType(Job.class, false, false)) {
            Class<?> declaringClass = getDeclaringClass(beanName);
            if (declaringClass != null) {
                result.computeIfAbsent(declaringClass.getName(), key -> new ArrayList<>()).add(beanName);
            }
        }
        return result;
    }

    /**
     * Job 빈을 정의한 클래스를 구한다 (CGLIB 프록시는 원래 클래스로 변환).
     * @param beanName Job 빈 이름
     * @return 정의 클래스, 알 수 없으면 null
     */
    private Class<?> getDeclaringClass(String beanName) {
        BeanDefinition definition = beanFactory.getMergedBeanDefinition(beanName);
        String factoryBeanName = definition.getFactoryBeanName();
        Class<?> type = beanFactory.getType(factoryBeanName != null ? factoryBeanName : beanName, false);
        return type == null ? null : ClassUtils.getUserClass(type);
    }
}
