package kr.or.coder.batch.frame.service;

import kr.or.coder.batch.frame.dto.BatchJobExecutionRequest;
import kr.or.coder.batch.frame.dto.BatchJobRequest;
import kr.or.coder.batch.frame.dto.BatchJobResponse;
import kr.or.coder.batch.frame.dto.BatchJobScheduleRequest;
import kr.or.coder.batch.frame.dto.BatchJobSearchRequest;

import java.util.List;

/**
 * 배치직무 등록/수정/삭제, 스케줄 제어, 수동 실행 Service
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
public interface BatchJobService {

    /**
     * 배치직무 목록을 스케줄 상태와 함께 조회한다.
     * @param request 배치직무 검색 조건
     * @return 배치직무 목록
     */
    List<BatchJobResponse> getBatchJobList(BatchJobSearchRequest request);

    /**
     * 배치직무 상세를 스케줄 상태와 함께 조회한다.
     * @param jobId 배치직무 ID
     * @return 배치직무 상세
     * @throws BatchException 배치직무가 없는 경우
     */
    BatchJobResponse getBatchJob(String jobId);

    /**
     * 배치직무를 저장하고 스케줄을 반영한다 (PK 존재 여부로 insert/update 분기).
     * @param request 배치직무 저장 요청
     * @param isNew 신규 등록 요청 여부 (true면 이미 있는 배치ID를 덮어쓰지 않고 오류)
     * @throws BatchException 신규 등록인데 이미 있는 배치ID이거나, Job 빈이 미등록이거나 스케줄 표현식/파라미터 형식이 올바르지 않은 경우
     */
    void saveBatchJob(BatchJobRequest request, boolean isNew);

    /**
     * 배치직무와 실행내역, Quartz 스케줄을 삭제한다.
     * @param jobId 배치직무 ID
     * @throws BatchException 배치직무가 없거나 클러스터에서 실행 중인 경우
     */
    void deleteBatchJob(String jobId);

    /**
     * 스케줄 일시정지/재개.
     * @param jobId 배치직무 ID
     * @param request 스케줄 상태 변경 요청
     * @throws BatchException 배치직무 또는 스케줄이 없거나 상태 값이 올바르지 않은 경우
     */
    void saveBatchJobSchedule(String jobId, BatchJobScheduleRequest request);

    /**
     * 수동 실행 요청 (Quartz 경유, 비동기).
     * @param jobId 배치직무 ID
     * @param request 수동 실행 요청 (생략 가능)
     * @throws BatchException 배치직무가 없거나 Job 빈이 미등록이거나 이미 실행 중인 경우
     */
    void saveBatchJobExecution(String jobId, BatchJobExecutionRequest request);
}
