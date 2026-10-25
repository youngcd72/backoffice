package kr.or.coder.batch.frame.mapper;

import kr.or.coder.batch.frame.dto.BatchExecutionResponse;
import kr.or.coder.batch.frame.dto.BatchExecutionSearchRequest;
import kr.or.coder.batch.frame.entity.BatchExecution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 배치 실행내역(TB_DSPT_EXEC_L) 조회/등록/수정/삭제 Mapper
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
public interface BatchExecutionMapper {

    /**
     * 검색 조건에 맞는 실행 이력 목록을 페이징 조회한다.
     * @param request 실행 이력 검색 조건
     * @return 실행 이력 목록
     */
    List<BatchExecutionResponse> selectBatchExecutionList(BatchExecutionSearchRequest request);

    /**
     * 검색 조건에 맞는 실행 이력 전체 건수를 조회한다.
     * @param request 실행 이력 검색 조건
     * @return 전체 건수
     */
    long selectBatchExecutionCount(BatchExecutionSearchRequest request);

    /**
     * 배치실행일련번호로 실행 이력 1건을 조회한다.
     * @param dsptExecSn 배치실행일련번호
     * @return 실행 이력, 없으면 null
     */
    BatchExecutionResponse selectBatchExecution(@Param("dsptExecSn") Long dsptExecSn);

    /**
     * 클러스터 전체에서 해당 Quartz Job이 실행 중인지(QRTZ_FIRED_TRIGGERS) 확인한다.
     * @param schedNm Quartz 스케줄러 이름
     * @param jobId 배치직무 ID
     * @param wrkMdulCd 업무모듈코드 (Quartz Job 그룹)
     * @return 실행 중인 Trigger 건수
     */
    int selectFiredTriggerCount(@Param("schedNm") String schedNm,
                                @Param("jobId") String jobId,
                                @Param("wrkMdulCd") String wrkMdulCd);

    /**
     * 실행 이력을 등록한다.
     * @param batchExecution 등록할 실행 이력
     * @return 등록 건수
     */
    int insertBatchExecution(BatchExecution batchExecution);

    /**
     * JOB_EXEC_ID 기준으로 종료 정보(상태, 종료일시, 건수, 오류메시지)를 갱신한다.
     * @param batchExecution 갱신할 종료 정보
     * @return 갱신 건수
     */
    int updateBatchExecution(BatchExecution batchExecution);

    /**
     * 배치직무 ID에 해당하는 실행 이력을 모두 삭제한다.
     * @param jobId 배치직무 ID
     * @return 삭제 건수
     */
    int deleteBatchExecutionByJobId(@Param("jobId") String jobId);
}
