package kr.or.coder.batch.job.system.execlogpurge;

import kr.or.coder.batch.common.config.BatchProperties;
import kr.or.coder.batch.job.system.execlogpurge.mapper.ExecLogPurgeBatchMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 배치 실행이력 정리 Tasklet (보관 기간이 지난 BATCH_* 메타 데이터와 TB_DSPT_EXEC_L 삭제)
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
@Slf4j
@Component
@StepScope
public class ExecLogPurgeTasklet implements Tasklet {

    private final ExecLogPurgeBatchMapper execLogPurgeBatchMapper;
    private final int retentionDays;

    /**
     * Tasklet을 생성한다. retentionDays JobParameter가 없으면 설정값을 보관 기간으로 사용한다.
     * @param execLogPurgeBatchMapper 실행이력 삭제 Mapper
     * @param batchProperties 배치 설정 프로퍼티
     * @param retentionDays 보관 기간(일) JobParameter, 없으면 빈 값
     */
    public ExecLogPurgeTasklet(ExecLogPurgeBatchMapper execLogPurgeBatchMapper,
                               BatchProperties batchProperties,
                               @Value("#{jobParameters['retentionDays']}") String retentionDays) {
        this.execLogPurgeBatchMapper = execLogPurgeBatchMapper;
        this.retentionDays = StringUtils.hasText(retentionDays)
                ? Integer.parseInt(retentionDays)
                : batchProperties.getRetentionDays();
    }

    /**
     * 보관 기간이 지난 실행이력을 FK 순서대로 삭제하고 삭제 건수를 Step의 쓰기 건수에 반영한다.
     * @param contribution Step 처리 건수 반영 객체
     * @param chunkContext Chunk 실행 컨텍스트
     * @return 한 번 실행 후 종료하므로 RepeatStatus.FINISHED
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDateTime baseDttm = LocalDate.now().minusDays(retentionDays).atStartOfDay();
        log.info("purge batch execution logs before {} (retentionDays={})", baseDttm, retentionDays);

        // FK 순서: Step 컨텍스트 → Step → Job 컨텍스트 → Job 파라미터 → Job 실행 → Job 인스턴스
        long deletedCount = 0;
        deletedCount += execLogPurgeBatchMapper.deleteBatchStepExecutionContext(baseDttm);
        deletedCount += execLogPurgeBatchMapper.deleteBatchStepExecution(baseDttm);
        deletedCount += execLogPurgeBatchMapper.deleteBatchJobExecutionContext(baseDttm);
        deletedCount += execLogPurgeBatchMapper.deleteBatchJobExecutionParams(baseDttm);
        deletedCount += execLogPurgeBatchMapper.deleteBatchJobExecution(baseDttm);
        deletedCount += execLogPurgeBatchMapper.deleteBatchJobInstance();
        deletedCount += execLogPurgeBatchMapper.deleteDsptExecLog(baseDttm);

        contribution.incrementWriteCount(deletedCount);
        log.info("purged {} row(s).", deletedCount);
        return RepeatStatus.FINISHED;
    }
}
