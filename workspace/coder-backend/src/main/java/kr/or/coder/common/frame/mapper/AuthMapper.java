package kr.or.coder.common.frame.mapper;

import kr.or.coder.common.frame.dto.auth.LoginUser;
import kr.or.coder.common.frame.dto.auth.UserProfileResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 사용자 조회 MyBatis Mapper
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
@Mapper
public interface AuthMapper {

    /**
     * 로그인 검증용 사용자 한 건을 조회 (비밀번호 해시, 사용여부, 잠금여부, 로그인오류건수, 사용기간, 역할)
     * @param usrId 사용자ID
     * @param adminGroupCodes 관리자 권한그룹 코드 목록
     * @return 로그인 검증용 사용자 (없으면 null)
     */
    LoginUser selectLoginUser(@Param("usrId") String usrId, @Param("adminGroupCodes") List<String> adminGroupCodes);

    /**
     * 전체 사용자 목록을 조회
     * @param adminGroupCodes 관리자 권한그룹 코드 목록
     * @return 사용자 프로필 목록
     */
    List<UserProfileResponse> selectUserList(@Param("adminGroupCodes") List<String> adminGroupCodes);

    /**
     * 로그인 실패 시 로그인오류건수를 1 올리고, 기준 횟수에 도달하면 계정을 잠금
     * @param usrId 사용자ID
     * @param maxLoginFailCount 계정을 잠그는 로그인오류건수
     * @return 수정 건수
     */
    int updateLoginFail(@Param("usrId") String usrId, @Param("maxLoginFailCount") int maxLoginFailCount);

    /**
     * 로그인 성공 시 로그인오류건수를 0으로 되돌리고 최종접속일시를 기록
     * @param usrId 사용자ID
     * @return 수정 건수
     */
    int updateLoginSuccess(@Param("usrId") String usrId);
}
