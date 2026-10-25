package kr.or.coder.batch.common.quartz;

import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * 모든 배치직무가 공유하는 Quartz Job (다중실행여부 MLT_EXEC_YN = 'Y').
 * 같은 JobKey의 Trigger가 겹치면 동시에 실행된다.
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
public class BatchLaunchQuartzJob extends QuartzJobBean {

    private final BatchLaunchExecutor batchLaunchExecutor;

    /**
     * Quartz Job을 생성한다.
     * @param batchLaunchExecutor 배치 Job 기동 실행기
     */
    public BatchLaunchQuartzJob(BatchLaunchExecutor batchLaunchExecutor) {
        this.batchLaunchExecutor = batchLaunchExecutor;
    }

    /**
     * Trigger 발생 시 배치 Job 기동을 실행기에 위임한다.
     * @param context Quartz 실행 컨텍스트
     */
    @Override
    protected void executeInternal(JobExecutionContext context) {
        batchLaunchExecutor.launch(context);
    }
}
