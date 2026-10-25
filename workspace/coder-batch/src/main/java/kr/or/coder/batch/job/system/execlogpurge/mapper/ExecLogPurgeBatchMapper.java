package kr.or.coder.batch.job.system.execlogpurge.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * execLogPurgeJob 전용 Mapper (BATCH_* 메타 테이블과 TB_DSPT_EXEC_L 삭제)
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
@Mapper
public interface ExecLogPurgeBatchMapper {

    /**
     * 기준일시 이전 Step 실행 컨텍스트를 삭제한다.
     * @param baseDttm 삭제 기준일시
     * @return 삭제 건수
     */
    int deleteBatchStepExecutionContext(@Param("baseDttm") LocalDateTime baseDttm);

    /**
     * 기준일시 이전 Step 실행 정보를 삭제한다.
     * @param baseDttm 삭제 기준일시
     * @return 삭제 건수
     */
    int deleteBatchStepExecution(@Param("baseDttm") LocalDateTime baseDttm);

    /**
     * 기준일시 이전 Job 실행 컨텍스트를 삭제한다.
     * @param baseDttm 삭제 기준일시
     * @return 삭제 건수
     */
    int deleteBatchJobExecutionContext(@Param("baseDttm") LocalDateTime baseDttm);

    /**
     * 기준일시 이전 Job 실행 파라미터를 삭제한다.
     * @param baseDttm 삭제 기준일시
     * @return 삭제 건수
     */
    int deleteBatchJobExecutionParams(@Param("baseDttm") LocalDateTime baseDttm);

    /**
     * 기준일시 이전 Job 실행 정보를 삭제한다.
     * @param baseDttm 삭제 기준일시
     * @return 삭제 건수
     */
    int deleteBatchJobExecution(@Param("baseDttm") LocalDateTime baseDttm);

    /**
     * 실행 건이 하나도 남지 않은 JobInstance를 삭제한다.
     * @return 삭제 건수
     */
    int deleteBatchJobInstance();

    /**
     * 기준일시 이전 배치실행내역(TB_DSPT_EXEC_L)을 삭제한다.
     * @param baseDttm 삭제 기준일시
     * @return 삭제 건수
     */
    int deleteDsptExecLog(@Param("baseDttm") LocalDateTime baseDttm);
}
