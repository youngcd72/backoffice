package kr.or.coder.biz.system.logs.service;

import kr.or.coder.biz.system.logs.dto.SystemLogPageResponse;
import kr.or.coder.biz.system.logs.dto.SystemLogRequest;
import kr.or.coder.biz.system.logs.dto.SystemLogResponse;

/**
 * SYLO030 시스템로그관리 Service (에러 로그 파일 logs/coder-backend-error.{일자}.{순번}.log 조회)
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
public interface SystemLogService {

    /**
     * 조회 조건에 맞는 시스템로그 목록을 최근 순으로 조회 (스택트레이스 제외, 최대 건수까지)
     * @param request 조회 조건 (기간, 시스템구분, 사용자구분, 사용자ID/명)
     * @return 시스템로그 목록, 전체 건수, 잘림 여부
     * @throws kr.or.coder.core.common.exception.BusinessException 기간이 없거나 형식이 틀렸거나 최대 기간을 넘는 경우
     */
    SystemLogPageResponse getSystemLogList(SystemLogRequest request);

    /**
     * 로그 키로 시스템로그 1건을 스택트레이스까지 조회
     * @param logKey 로그 키 ({일자}-{파일순번}-{줄번호})
     * @return 시스템로그 상세
     * @throws kr.or.coder.core.common.exception.BusinessException 키 형식이 틀렸거나 로그가 없는 경우
     */
    SystemLogResponse getSystemLog(String logKey);
}
