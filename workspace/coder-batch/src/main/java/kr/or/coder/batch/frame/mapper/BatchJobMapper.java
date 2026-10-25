package kr.or.coder.batch.frame.mapper;

import kr.or.coder.batch.frame.dto.BatchJobSearchRequest;
import kr.or.coder.batch.frame.entity.BatchJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 배치직무기본(TB_DSPT_JOB_M) 조회/등록/수정/삭제 Mapper
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
public interface BatchJobMapper {

    /**
     * 검색 조건에 맞는 배치직무 목록을 조회한다.
     * @param request 배치직무 검색 조건
     * @return 배치직무 목록
     */
    List<BatchJob> selectBatchJobList(BatchJobSearchRequest request);

    /**
     * 배치직무 1건을 조회한다.
     * @param jobId 배치직무 ID
     * @return 배치직무, 없으면 null
     */
    BatchJob selectBatchJob(@Param("jobId") String jobId);

    /**
     * 배치직무를 등록한다.
     * @param batchJob 등록할 배치직무
     * @return 등록 건수
     */
    int insertBatchJob(BatchJob batchJob);

    /**
     * 배치직무를 수정한다.
     * @param batchJob 수정할 배치직무
     * @return 수정 건수
     */
    int updateBatchJob(BatchJob batchJob);

    /**
     * 배치직무를 삭제한다.
     * @param jobId 배치직무 ID
     * @return 삭제 건수
     */
    int deleteBatchJob(@Param("jobId") String jobId);
}
