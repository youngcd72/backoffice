package kr.or.coder.biz.system.link.service;

import kr.or.coder.biz.system.link.dto.InterfaceRequest;
import kr.or.coder.biz.system.link.dto.InterfaceResponse;
import kr.or.coder.biz.system.link.dto.InterfaceSaveRequest;

import java.util.List;

/**
 * SYLK010 인터페이스 Service
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
public interface InterfaceService {

    /**
     * 조회 조건에 맞는 인터페이스 목록을 조회 (허용 기관 수 포함)
     * @param request 조회 조건
     * @return 인터페이스 목록
     */
    List<InterfaceResponse> getInterfaceList(InterfaceRequest request);

    /**
     * 인터페이스를 등록 또는 수정
     * @param request 인터페이스 (isNew 로 등록/수정 구분)
     * @param usrId 저장 사용자ID
     */
    void saveInterface(InterfaceSaveRequest request, String usrId);
}
