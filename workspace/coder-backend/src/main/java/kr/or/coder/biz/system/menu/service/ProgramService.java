package kr.or.coder.biz.system.menu.service;

import kr.or.coder.biz.system.menu.dto.ProgramComponentResponse;
import kr.or.coder.biz.system.menu.dto.ProgramRequest;
import kr.or.coder.biz.system.menu.dto.ProgramResponse;
import kr.or.coder.biz.system.menu.dto.ProgramSaveRequest;

import java.util.List;

/**
 * SYPG010 프로그램관리 Service
 * @author ywkim
 * @since 2026.10.17
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.17  ywkim        최초 생성
 * </pre>
 */
public interface ProgramService {

    /**
     * 조회 조건에 맞는 프로그램 목록을 정렬순번 순으로 조회
     * @param request 조회 조건 (프로그램경로코드, 프로그램ID, 프로그램명, 사용여부)
     * @return 프로그램 목록
     */
    List<ProgramResponse> getProgramList(ProgramRequest request);

    /**
     * 프로그램의 컴포넌트 목록을 정렬순번 순으로 조회
     * @param prgmId 프로그램ID
     * @return 컴포넌트 목록
     */
    List<ProgramComponentResponse> getProgramComponentList(String prgmId);

    /**
     * 프로그램·컴포넌트 변경분(행 상태 I/U/D 인 항목)을 한 트랜잭션으로 저장
     * 순서: 프로그램 삭제(소속 컴포넌트 포함) → 수정 → 등록 → 컴포넌트 삭제 → 수정 → 등록
     * @param request 프로그램·컴포넌트 변경분
     * @param usrId 저장 사용자ID
     * @return 저장 건수
     * @throws kr.or.coder.core.common.exception.BusinessException 등록 대상이 이미 있거나 수정 대상이 없는 경우
     */
    int saveProgramList(ProgramSaveRequest request, String usrId);
}
