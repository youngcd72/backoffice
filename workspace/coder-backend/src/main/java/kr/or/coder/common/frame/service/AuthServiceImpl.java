package kr.or.coder.common.frame.service;

import kr.or.coder.common.auth.AuthProperties;
import kr.or.coder.common.frame.dto.auth.LoginRequest;
import kr.or.coder.common.frame.dto.auth.LoginResponse;
import kr.or.coder.common.frame.dto.auth.LoginUser;
import kr.or.coder.common.frame.dto.auth.UserProfileResponse;
import kr.or.coder.common.frame.mapper.AuthMapper;
import kr.or.coder.core.common.exception.BusinessException;
import kr.or.coder.core.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 로그인 인증 및 사용자 목록 조회 Service 구현
 * @author ywkim
 * @since 2026.10.05
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.05  ywkim        최초 생성
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String YES = "Y";
    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

    private final AuthMapper authMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    /**
     * 사용자 아이디/비밀번호와 계정 상태를 검증하고 역할별 권한을 부여한 뒤 JWT 토큰을 발급
     * 비밀번호가 틀리면 로그인오류건수를 올리고, 기준 횟수에 도달하면 계정을 잠근다.
     * @param request 로그인 요청 (아이디, 비밀번호)
     * @return 토큰과 사용자 정보
     * @throws BusinessException 아이디·비밀번호가 맞지 않거나(sy.login.credentials.mismatch), 잠긴 계정이거나(sy.login.account.locked),
     *                           이번 실패로 잠긴 경우(sy.login.account.locked.now), 미사용·사용기간 밖 계정인 경우(sy.login.account.unavailable)
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        String usrId = request.getUsername();
        LoginUser user = authMapper.selectLoginUser(usrId, authProperties.getAdminGroupCodes());
        if (user == null) {
            log.warn("[AuthService] User not found: {}", usrId);
            throw new BusinessException("sy.login.credentials.mismatch");
        }
        if (YES.equals(user.getUsrLockYn())) {
            log.warn("[AuthService] Locked user login attempt: {}", usrId);
            throw new BusinessException("sy.login.account.locked");
        }
        if (!StringUtils.hasText(user.getPwd()) || !passwordEncoder.matches(request.getPassword(), user.getPwd())) {
            handleLoginFail(user);
        }
        if (!isUsable(user)) {
            log.warn("[AuthService] Unavailable user login attempt: {}", usrId);
            throw new BusinessException("sy.login.account.unavailable");
        }
        authMapper.updateLoginSuccess(usrId);

        List<String> permissions = getPermissionList(user.getRole());
        String jwtToken = jwtTokenProvider.generateToken(usrId, user.getUsrNm(), user.getRole(), permissions, user.getEmlAddr());
        UserProfileResponse profile = UserProfileResponse.builder()
                .username(usrId)
                .name(user.getUsrNm())
                .email(user.getEmlAddr())
                .role(user.getRole())
                .status("ACTIVE")
                .permissions(permissions)
                .build();

        return LoginResponse.builder()
                .accessToken(jwtToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getProperties().getAccessTokenExpiration())
                .user(profile)
                .build();
    }

    /**
     * 전체 사용자 목록을 조회 (사용자 코드서치용)
     * @return 사용자 프로필 목록
     */
    @Override
    public List<UserProfileResponse> getUserList() {
        return authMapper.selectUserList(authProperties.getAdminGroupCodes());
    }

    /**
     * 비밀번호 불일치 시 로그인오류건수를 올리고 예외를 던짐 (기준 횟수에 도달하면 계정 잠금)
     * @param user 로그인 검증용 사용자
     * @throws BusinessException 이번 실패로 잠긴 경우 sy.login.account.locked.now, 그 외 sy.login.credentials.mismatch
     */
    private void handleLoginFail(LoginUser user) {
        int maxCnt = authProperties.getMaxLoginFailCount();
        authMapper.updateLoginFail(user.getUsrId(), maxCnt);
        int failCnt = (user.getLginErrCnt() == null ? 0 : user.getLginErrCnt()) + 1;
        log.warn("[AuthService] Password mismatch: usrId={}, failCnt={}", user.getUsrId(), failCnt);
        if (failCnt >= maxCnt) {
            throw new BusinessException("sy.login.account.locked.now", maxCnt);
        }
        throw new BusinessException("sy.login.credentials.mismatch");
    }

    /**
     * 사용 중이고 오늘이 사용기간(시작일자~종료일자) 안인 계정인지 판별
     * @param user 로그인 검증용 사용자
     * @return 로그인할 수 있는 계정이면 true
     */
    private boolean isUsable(LoginUser user) {
        String today = LocalDate.now().format(YMD);
        return YES.equals(user.getUseYn())
                && (!StringUtils.hasText(user.getUseStDt()) || user.getUseStDt().compareTo(today) <= 0)
                && (!StringUtils.hasText(user.getUseEndDt()) || user.getUseEndDt().compareTo(today) >= 0);
    }

    /**
     * 역할별 권한 목록을 반환 (JWT permissions 클레임)
     * @param role 역할 (ADMIN, USER)
     * @return 권한 목록
     */
    private List<String> getPermissionList(String role) {
        if (ROLE_ADMIN.equals(role)) {
            return List.of("ALL", "SYSTEM_ADMIN", "READ_MODULES", "WRITE_MODULES", "CACHE_ADMIN");
        }
        return List.of("READ_MODULES", "VIEW_DASHBOARD");
    }
}
