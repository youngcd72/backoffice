package kr.or.coder.biz.system.auths.service;

import kr.or.coder.biz.system.auths.dto.AuthGroupComponentResponse;
import kr.or.coder.biz.system.auths.dto.AuthGroupMenuResponse;
import kr.or.coder.biz.system.auths.dto.AuthGroupRequest;
import kr.or.coder.biz.system.auths.dto.AuthGroupResponse;
import kr.or.coder.biz.system.auths.dto.AuthGroupSaveRequest;
import kr.or.coder.biz.system.auths.dto.AuthGroupUserCountResponse;
import kr.or.coder.biz.system.auths.dto.AuthGroupUserResponse;
import kr.or.coder.biz.system.auths.dto.AuthGroupUserSaveRequest;

import java.util.List;

/**
 * SYAU020 그룹권한관리 Service
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
public interface AuthGroupService {

    /**
     * 조회 조건에 맞는 권한그룹 목록을 그룹코드 순으로 조회
     * @param request 조회 조건 (그룹코드, 그룹명)
     * @return 권한그룹 목록
     */
    List<AuthGroupResponse> getAuthGroupList(AuthGroupRequest request);

    /**
     * 권한그룹의 화면 권한 목록을 조회
     * @param usrGrpCd 그룹코드
     * @return 화면 권한 목록
     */
    List<AuthGroupMenuResponse> getAuthGroupMenuList(String usrGrpCd);

    /**
     * 권한그룹의 컴포넌트 권한을 조회 (프로그램ID가 없으면 전체)
     * @param usrGrpCd 그룹코드
     * @param prgmId 프로그램ID (없으면 전체)
     * @return 컴포넌트 권한 목록
     */
    List<AuthGroupComponentResponse> getAuthGroupComponentList(String usrGrpCd, String prgmId);

    /**
     * 권한그룹·화면 권한·컴포넌트 권한 변경분(행 상태 I/U/D 인 항목)을 한 트랜잭션으로 저장
     * 순서: 그룹 삭제(그룹 사용자·화면·컴포넌트 권한 포함) → 그룹 수정 → 그룹 등록 → 화면 권한 삭제·등록 → 컴포넌트 권한 등록·수정
     * @param request 권한그룹·화면 권한·컴포넌트 권한 변경분
     * @param regrId 저장 사용자ID
     * @return 저장 건수
     * @throws kr.or.coder.core.common.exception.BusinessException 등록 대상 그룹이 이미 있거나 수정 대상이 없는 경우
     */
    int saveAuthGroupList(AuthGroupSaveRequest request, String regrId);

    /**
     * 조회 조건에 맞는 권한그룹 목록을 그룹별 사용자수와 함께 조회
     * @param request 조회 조건 (그룹코드, 그룹명, 사용여부)
     * @return 권한그룹·사용자수 목록
     */
    List<AuthGroupUserCountResponse> getAuthGroupUserCountList(AuthGroupRequest request);

    /**
     * 권한그룹에 속한 사용자 목록을 조회
     * @param usrGrpCd 그룹코드
     * @return 그룹 사용자 목록
     */
    List<AuthGroupUserResponse> getAuthGroupUserList(String usrGrpCd);

    /**
     * 권한그룹에 사용자를 추가하거나 제외 (한 트랜잭션)
     * @param usrGrpCd 그룹코드
     * @param request 추가/제외할 사용자ID 목록
     * @param regrId 저장 사용자ID
     * @return 처리 건수
     * @throws kr.or.coder.core.common.exception.BusinessException 그룹이 없는 경우
     */
    int saveAuthGroupUserList(String usrGrpCd, AuthGroupUserSaveRequest request, String regrId);
}
