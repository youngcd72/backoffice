package kr.or.coder.batch.common.quartz;

import org.quartz.DisallowConcurrentExecution;

/**
 * 다중실행여부(MLT_EXEC_YN) = 'N'인 배치직무용 Quartz Job.
 * 같은 JobKey는 클러스터 전체에서 동시에 한 번만 실행되고, 겹친 Trigger는 앞 실행이 끝날 때까지 대기(BLOCKED)한다.
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
@DisallowConcurrentExecution
public class SerialBatchLaunchQuartzJob extends BatchLaunchQuartzJob {

    /**
     * 직렬 실행 Quartz Job을 생성한다.
     * @param batchLaunchExecutor 배치 Job 기동 실행기
     */
    public SerialBatchLaunchQuartzJob(BatchLaunchExecutor batchLaunchExecutor) {
        super(batchLaunchExecutor);
    }
}
