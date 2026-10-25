package kr.or.coder.batch.frame.service;

import kr.or.coder.batch.common.support.PageResponse;
import kr.or.coder.batch.frame.dto.BatchExecutionResponse;
import kr.or.coder.batch.frame.dto.BatchExecutionSearchRequest;
import kr.or.coder.batch.frame.dto.BatchExecutionStatusRequest;

/**
 * 배치 실행 이력 조회/재시작/중지 Service
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
public interface BatchExecutionService {

    /**
     * 실행 이력 목록을 페이징 조회한다.
     * @param request 실행 이력 검색 조건
     * @return 실행 이력 페이지 응답
     */
    PageResponse<BatchExecutionResponse> getBatchExecutionList(BatchExecutionSearchRequest request);

    /**
     * 실행 상세 (JobParameters, Step별 처리 현황 포함).
     * @param dsptExecSn 배치실행일련번호
     * @return 실행 이력 상세
     * @throws BatchException 실행 이력이 없는 경우
     */
    BatchExecutionResponse getBatchExecution(Long dsptExecSn);

    /**
     * FAILED / STOPPED 실행 건을 같은 JobParameters로 재시작한다 (Quartz 경유, 비동기).
     * @param dsptExecSn 배치실행일련번호
     * @throws BatchException 실행 이력이 없거나 재시작할 수 없는 상태인 경우
     */
    void saveBatchExecutionRestart(Long dsptExecSn);

    /**
     * 실행 중지(STOPPED) 또는 폐기(ABANDONED).
     * @param dsptExecSn 배치실행일련번호
     * @param request 변경할 실행상태 요청
     * @throws BatchException 실행 이력이 없거나 중지/폐기할 수 없는 상태인 경우
     */
    void saveBatchExecutionStatus(Long dsptExecSn, BatchExecutionStatusRequest request);
}
