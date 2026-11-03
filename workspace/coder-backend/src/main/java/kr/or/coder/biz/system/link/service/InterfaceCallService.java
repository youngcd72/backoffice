package kr.or.coder.biz.system.link.service;

import kr.or.coder.biz.system.link.dto.InterfaceCallPageResponse;
import kr.or.coder.biz.system.link.dto.InterfaceCallRequest;
import kr.or.coder.biz.system.link.dto.InterfaceCallResponse;

/**
 * SYLK010 호출이력 Service
 * @author ywkim
 * @since 2026.11.03
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.11.03  ywkim        최초 생성
 * </pre>
 */
public interface InterfaceCallService {

    /**
     * 조회 조건에 맞는 호출이력을 요청일시 최근 순으로 최대 건수까지 조회
     * @param request 조회 조건 (요청일자 범위 필수)
     * @return 호출이력 목록, 전체 건수, 잘림 여부
     */
    InterfaceCallPageResponse getInterfaceCallList(InterfaceCallRequest request);

    /**
     * 호출이력 1건을 요청·응답 본문까지 조회
     * @param callSn 호출일련번호
     * @param rqmtDttm 요청일시 (YYYY-MM-DDTHH:mm:ss.SSS, 파티션 키)
     * @return 호출이력
     */
    InterfaceCallResponse getInterfaceCall(long callSn, String rqmtDttm);
}
