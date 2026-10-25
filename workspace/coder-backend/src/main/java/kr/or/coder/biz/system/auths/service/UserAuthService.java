package kr.or.coder.biz.system.auths.service;

import kr.or.coder.biz.system.auths.dto.UserAuthRequest;
import kr.or.coder.biz.system.auths.dto.UserAuthResponse;
import kr.or.coder.biz.system.auths.dto.UserAuthSaveRequest;
import kr.or.coder.biz.system.auths.dto.UserComponentAuthResponse;
import kr.or.coder.biz.system.auths.dto.UserMenuAuthResponse;

import java.util.List;

/**
 * SYAU010 사용자권한관리 Service
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
public interface UserAuthService {

    /**
     * 조회 조건에 맞는 사용자 목록을 사용자ID 순으로 조회
     * @param request 조회 조건 (사용자ID, 사용자명, 사용자구분, 사용여부)
     * @return 사용자 목록
     */
    List<UserAuthResponse> getUserAuthList(UserAuthRequest request);

    /**
     * 사용자ID가 등록되어 있는지 판별
     * @param usrId 사용자ID
     * @return 등록되어 있으면 true
     */
    boolean existsUser(String usrId);

    /**
     * 사용자의 화면 권한 목록을 조회
     * @param usrId 사용자ID
     * @return 화면 권한 목록
     */
    List<UserMenuAuthResponse> getUserMenuAuthList(String usrId);

    /**
     * 사용자의 컴포넌트 권한을 조회 (프로그램ID가 없으면 전체)
     * @param usrId 사용자ID
     * @param prgmId 프로그램ID (없으면 전체)
     * @return 컴포넌트 권한 목록
     */
    List<UserComponentAuthResponse> getUserComponentAuthList(String usrId, String prgmId);

    /**
     * 사용자·화면 권한·컴포넌트 권한 변경분(행 상태 I/U/D 인 항목)을 한 트랜잭션으로 저장
     * 순서: 사용자 삭제(권한그룹·즐겨찾기·화면·컴포넌트 권한 포함) → 사용자 수정 → 사용자 등록 → 화면 권한 삭제·등록 → 컴포넌트 권한 등록·수정
     * @param request 사용자·화면 권한·컴포넌트 권한 변경분
     * @param regrId 저장 사용자ID
     * @return 저장 건수
     * @throws kr.or.coder.core.common.exception.BusinessException 등록 대상 사용자가 이미 있거나 수정 대상이 없는 경우
     */
    int saveUserAuthList(UserAuthSaveRequest request, String regrId);

    /**
     * 사용자 비밀번호를 초기 비밀번호로 초기화 (BCrypt 해시 저장, 최초로그인여부 Y, 로그인오류건수 0, 잠금 해제)
     * @param usrId 사용자ID
     * @param regrId 처리 사용자ID
     * @throws kr.or.coder.core.common.exception.BusinessException 사용자가 없는 경우
     */
    void saveUserPasswordReset(String usrId, String regrId);
}
