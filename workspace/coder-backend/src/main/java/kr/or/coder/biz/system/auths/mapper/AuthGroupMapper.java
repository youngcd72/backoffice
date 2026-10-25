package kr.or.coder.biz.system.auths.mapper;

import kr.or.coder.biz.system.auths.dto.AuthGroupComponentItemRequest;
import kr.or.coder.biz.system.auths.dto.AuthGroupComponentResponse;
import kr.or.coder.biz.system.auths.dto.AuthGroupItemRequest;
import kr.or.coder.biz.system.auths.dto.AuthGroupMenuItemRequest;
import kr.or.coder.biz.system.auths.dto.AuthGroupMenuResponse;
import kr.or.coder.biz.system.auths.dto.AuthGroupRequest;
import kr.or.coder.biz.system.auths.dto.AuthGroupResponse;
import kr.or.coder.biz.system.auths.dto.AuthGroupUserCountResponse;
import kr.or.coder.biz.system.auths.dto.AuthGroupUserResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SYAU020 그룹권한관리 MyBatis Mapper (TB_AUTH_GRP_M, TB_AUTH_GRP_MENU_R, TB_AUTH_GRP_CMPN_R)
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
public interface AuthGroupMapper {

    /**
     * 조회 조건에 맞는 권한그룹 목록을 그룹코드 순으로 조회
     * @param request 조회 조건 (그룹코드, 그룹명)
     * @return 권한그룹 목록
     */
    List<AuthGroupResponse> selectAuthGroupList(AuthGroupRequest request);

    /**
     * 권한그룹이 등록되어 있는지 판별
     * @param usrGrpCd 그룹코드
     * @return 등록되어 있으면 true
     */
    boolean existsAuthGroup(@Param("usrGrpCd") String usrGrpCd);

    /**
     * 권한그룹을 등록
     * @param item 권한그룹 1건
     * @param regrId 등록/수정 사용자ID
     * @return 등록 건수
     */
    int insertAuthGroup(@Param("item") AuthGroupItemRequest item, @Param("regrId") String regrId);

    /**
     * 권한그룹을 수정
     * @param item 권한그룹 1건
     * @param regrId 등록/수정 사용자ID
     * @return 수정 건수
     */
    int updateAuthGroup(@Param("item") AuthGroupItemRequest item, @Param("regrId") String regrId);

    /**
     * 권한그룹을 삭제
     * @param usrGrpCd 그룹코드
     * @return 삭제 건수
     */
    int deleteAuthGroup(@Param("usrGrpCd") String usrGrpCd);

    /**
     * 권한그룹의 사용자 매핑을 모두 삭제 (그룹 삭제 시)
     * @param usrGrpCd 그룹코드
     * @return 삭제 건수
     */
    int deleteAuthGroupUserList(@Param("usrGrpCd") String usrGrpCd);

    /**
     * 권한그룹의 화면 권한 목록을 메뉴ID 순으로 조회
     * @param usrGrpCd 그룹코드
     * @return 화면 권한 목록
     */
    List<AuthGroupMenuResponse> selectAuthGroupMenuList(@Param("usrGrpCd") String usrGrpCd);


    /**
     * 권한그룹 화면 권한을 등록 (이미 있으면 건너뜀)
     * @param item 화면 권한 1건
     * @param regrId 등록/수정 사용자ID
     * @return 등록 건수
     */
    int insertAuthGroupMenu(@Param("item") AuthGroupMenuItemRequest item, @Param("regrId") String regrId);

    /**
     * 권한그룹 화면 권한을 삭제
     * @param usrGrpCd 그룹코드
     * @param menuId 메뉴ID
     * @param prgmId 프로그램ID
     * @return 삭제 건수
     */
    int deleteAuthGroupMenu(@Param("usrGrpCd") String usrGrpCd, @Param("menuId") String menuId, @Param("prgmId") String prgmId);

    /**
     * 권한그룹의 화면 권한을 모두 삭제 (그룹 삭제 시)
     * @param usrGrpCd 그룹코드
     * @return 삭제 건수
     */
    int deleteAuthGroupMenuList(@Param("usrGrpCd") String usrGrpCd);

    /**
     * 권한그룹의 컴포넌트 권한을 조회 (프로그램ID가 없으면 전체)
     * @param usrGrpCd 그룹코드
     * @param prgmId 프로그램ID (null 이면 전체)
     * @return 컴포넌트 권한 목록
     */
    List<AuthGroupComponentResponse> selectAuthGroupComponentList(@Param("usrGrpCd") String usrGrpCd, @Param("prgmId") String prgmId);


    /**
     * 권한그룹 컴포넌트 권한을 등록 (이미 있으면 권한여부 수정)
     * @param item 컴포넌트 권한 1건
     * @param regrId 등록/수정 사용자ID
     * @return 등록 건수
     */
    int insertAuthGroupComponent(@Param("item") AuthGroupComponentItemRequest item, @Param("regrId") String regrId);


    /**
     * 권한그룹의 컴포넌트 권한을 모두 삭제 (그룹 삭제 시)
     * @param usrGrpCd 그룹코드
     * @return 삭제 건수
     */
    int deleteAuthGroupComponentList(@Param("usrGrpCd") String usrGrpCd);

    /**
     * 화면 권한이 남아 있지 않은 프로그램의 컴포넌트 권한을 삭제
     * @param usrGrpCd 그룹코드
     * @return 삭제 건수
     */
    int deleteAuthGroupComponentWithoutMenu(@Param("usrGrpCd") String usrGrpCd);

    /**
     * 조회 조건에 맞는 권한그룹 목록을 그룹별 사용자수와 함께 그룹코드 순으로 조회
     * @param request 조회 조건 (그룹코드, 그룹명, 사용여부)
     * @return 권한그룹·사용자수 목록
     */
    List<AuthGroupUserCountResponse> selectAuthGroupUserCountList(AuthGroupRequest request);

    /**
     * 권한그룹에 속한 사용자 목록을 사용자ID 순으로 조회
     * @param usrGrpCd 그룹코드
     * @return 그룹 사용자 목록
     */
    List<AuthGroupUserResponse> selectAuthGroupUserList(@Param("usrGrpCd") String usrGrpCd);

    /**
     * 권한그룹에 사용자를 추가 (이미 있으면 무시, 사용기간 오늘~99991231)
     * @param usrGrpCd 그룹코드
     * @param usrId 사용자ID
     * @param regrId 등록 사용자ID
     * @return 등록 건수
     */
    int insertAuthGroupUser(@Param("usrGrpCd") String usrGrpCd, @Param("usrId") String usrId, @Param("regrId") String regrId);

    /**
     * 권한그룹에서 사용자를 제외
     * @param usrGrpCd 그룹코드
     * @param usrId 사용자ID
     * @return 삭제 건수
     */
    int deleteAuthGroupUser(@Param("usrGrpCd") String usrGrpCd, @Param("usrId") String usrId);
}
