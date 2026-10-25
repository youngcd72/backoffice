package kr.or.coder.batch.common.listener;

import kr.or.coder.batch.common.support.BatchConstants;
import org.slf4j.MDC;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Step 실행 동안 로그 MDC에 stepName을 넣는다.
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
@Component
public class StepMdcListener implements StepExecutionListener {

    /**
     * Step 시작 시 MDC에 Step명을 설정한다.
     * @param stepExecution 시작되는 Step 실행 정보
     */
    @Override
    public void beforeStep(StepExecution stepExecution) {
        MDC.put(BatchConstants.MDC_STEP_NAME, stepExecution.getStepName());
    }

    /**
     * Step 종료 시 MDC의 Step명을 제거한다.
     * @param stepExecution 종료된 Step 실행 정보
     * @return 종료 상태를 변경하지 않으므로 null
     */
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        MDC.remove(BatchConstants.MDC_STEP_NAME);
        return null;
    }
}
