package kr.or.coder.biz.system.auths.mapper;

import kr.or.coder.biz.system.auths.dto.UserAuthItemRequest;
import kr.or.coder.biz.system.auths.dto.UserAuthRequest;
import kr.or.coder.biz.system.auths.dto.UserAuthResponse;
import kr.or.coder.biz.system.auths.dto.UserComponentAuthItemRequest;
import kr.or.coder.biz.system.auths.dto.UserComponentAuthResponse;
import kr.or.coder.biz.system.auths.dto.UserMenuAuthItemRequest;
import kr.or.coder.biz.system.auths.dto.UserMenuAuthResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SYAU010 사용자권한관리 MyBatis Mapper (TB_USR_M, TB_USR_MENU_R, TB_USR_CMPN_R)
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
@Mapper
public interface UserAuthMapper {

    /**
     * 조회 조건에 맞는 사용자 목록을 사용자ID 순으로 조회
     * @param request 조회 조건 (사용자ID, 사용자명, 사용자구분, 사용여부)
     * @return 사용자 목록
     */
    List<UserAuthResponse> selectUserAuthList(UserAuthRequest request);

    /**
     * 사용자ID가 등록되어 있는지 판별
     * @param usrId 사용자ID
     * @return 등록되어 있으면 true
     */
    boolean existsUser(@Param("usrId") String usrId);

    /**
     * 사용자를 등록 (최초로그인여부 Y, 로그인오류건수 0)
     * @param item 사용자 1건
     * @param pwd 초기 비밀번호 (BCrypt 해시)
     * @param regrId 등록/수정 사용자ID
     * @return 등록 건수
     */
    int insertUser(@Param("item") UserAuthItemRequest item, @Param("pwd") String pwd, @Param("regrId") String regrId);

    /**
     * 사용자를 수정
     * @param item 사용자 1건
     * @param regrId 등록/수정 사용자ID
     * @return 수정 건수
     */
    int updateUser(@Param("item") UserAuthItemRequest item, @Param("regrId") String regrId);

    /**
     * 사용자를 삭제
     * @param usrId 사용자ID
     * @return 삭제 건수
     */
    int deleteUser(@Param("usrId") String usrId);

    /**
     * 사용자의 권한그룹 매핑을 모두 삭제 (사용자 삭제 시)
     * @param usrId 사용자ID
     * @return 삭제 건수
     */
    int deleteUserAuthGroupList(@Param("usrId") String usrId);

    /**
     * 사용자의 즐겨찾기를 모두 삭제 (사용자 삭제 시)
     * @param usrId 사용자ID
     * @return 삭제 건수
     */
    int deleteUserFavoriteList(@Param("usrId") String usrId);

    /**
     * 사용자 비밀번호를 초기 비밀번호로 초기화 (최초로그인여부 Y, 로그인오류건수 0, 잠금 해제)
     * @param usrId 사용자ID
     * @param pwd 초기 비밀번호 (BCrypt 해시)
     * @param regrId 등록/수정 사용자ID
     * @return 수정 건수
     */
    int updateUserPasswordReset(@Param("usrId") String usrId, @Param("pwd") String pwd,
                                @Param("regrId") String regrId);

    /**
     * 사용자의 화면 권한 목록을 메뉴ID 순으로 조회
     * @param usrId 사용자ID
     * @return 화면 권한 목록
     */
    List<UserMenuAuthResponse> selectUserMenuAuthList(@Param("usrId") String usrId);


    /**
     * 사용자 화면 권한을 등록 (이미 있으면 건너뜀)
     * @param item 화면 권한 1건
     * @param regrId 등록/수정 사용자ID
     * @return 등록 건수
     */
    int insertUserMenuAuth(@Param("item") UserMenuAuthItemRequest item, @Param("regrId") String regrId);

    /**
     * 사용자 화면 권한을 삭제
     * @param usrId 사용자ID
     * @param menuId 메뉴ID
     * @param prgmId 프로그램ID
     * @return 삭제 건수
     */
    int deleteUserMenuAuth(@Param("usrId") String usrId, @Param("menuId") String menuId, @Param("prgmId") String prgmId);

    /**
     * 사용자의 화면 권한을 모두 삭제 (사용자 삭제 시)
     * @param usrId 사용자ID
     * @return 삭제 건수
     */
    int deleteUserMenuAuthList(@Param("usrId") String usrId);

    /**
     * 사용자의 컴포넌트 권한을 조회 (프로그램ID가 없으면 전체)
     * @param usrId 사용자ID
     * @param prgmId 프로그램ID (null 이면 전체)
     * @return 컴포넌트 권한 목록
     */
    List<UserComponentAuthResponse> selectUserComponentAuthList(@Param("usrId") String usrId, @Param("prgmId") String prgmId);


    /**
     * 사용자 컴포넌트 권한을 등록 (이미 있으면 권한여부 수정)
     * @param item 컴포넌트 권한 1건
     * @param regrId 등록/수정 사용자ID
     * @return 등록 건수
     */
    int insertUserComponentAuth(@Param("item") UserComponentAuthItemRequest item, @Param("regrId") String regrId);


    /**
     * 사용자의 컴포넌트 권한을 모두 삭제 (사용자 삭제 시)
     * @param usrId 사용자ID
     * @return 삭제 건수
     */
    int deleteUserComponentAuthList(@Param("usrId") String usrId);

    /**
     * 화면 권한이 남아 있지 않은 프로그램의 컴포넌트 권한을 삭제
     * @param usrId 사용자ID
     * @return 삭제 건수
     */
    int deleteUserComponentAuthWithoutMenu(@Param("usrId") String usrId);

    /**
     * 사용자가 받은 알람을 모두 삭제 (사용자 삭제 시)
     * @param usrId 사용자ID
     * @return 삭제 건수
     */
    int deleteUserAlarmList(@Param("usrId") String usrId);

    /**
     * 사용자를 배치 알람수신자에서 모두 삭제 (사용자 삭제 시)
     * @param usrId 사용자ID
     * @return 삭제 건수
     */
    int deleteUserBatchAlarmReceiverList(@Param("usrId") String usrId);
}
