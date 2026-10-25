package kr.or.coder.common.frame.service;

import kr.or.coder.common.frame.dto.auth.LoginRequest;
import kr.or.coder.common.frame.dto.auth.LoginResponse;
import kr.or.coder.common.frame.dto.auth.UserProfileResponse;
import kr.or.coder.core.common.exception.BusinessException;

import java.util.List;

/**
 * 로그인 인증 및 사용자 목록 조회 Service
 * @author ywkim
 * @since 2026.10.20
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.20  ywkim        최초 생성 (구체 클래스 AuthService를 인터페이스와 구현으로 분리)
 * </pre>
 */
public interface AuthService {

    /**
     * 사용자 아이디/비밀번호와 계정 상태를 검증하고 역할별 권한을 부여한 뒤 JWT 토큰을 발급
     * 비밀번호가 틀리면 로그인오류건수를 올리고, 기준 횟수에 도달하면 계정을 잠근다.
     * @param request 로그인 요청 (아이디, 비밀번호)
     * @return 토큰과 사용자 정보
     * @throws BusinessException 아이디·비밀번호가 맞지 않거나(sy.login.credentials.mismatch), 잠긴 계정이거나(sy.login.account.locked),
     *                           이번 실패로 잠긴 경우(sy.login.account.locked.now), 미사용·사용기간 밖 계정인 경우(sy.login.account.unavailable)
     */
    LoginResponse login(LoginRequest request);

    /**
     * 전체 사용자 목록을 조회 (사용자 코드서치용)
     * @return 사용자 프로필 목록
     */
    List<UserProfileResponse> getUserList();
}
