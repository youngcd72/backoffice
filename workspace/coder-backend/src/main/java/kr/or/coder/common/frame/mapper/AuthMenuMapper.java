package kr.or.coder.common.frame.mapper;

import kr.or.coder.common.frame.dto.auth.AuthMenuResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 로그인 사용자의 권한 메뉴(TB_MENU_M, TB_AUTH_GRP_MENU_R, TB_USR_MENU_R) MyBatis Mapper
 * @author ywkim
 * @since 2026.10.19
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.19  ywkim        최초 생성
 * </pre>
 */
@Mapper
public interface AuthMenuMapper {

    /**
     * 사용자에게 권한이 있는 화면 메뉴와 그 상위 메뉴를 조회 (적용기간 내 권한그룹 또는 사용자 권한, 사용 중인 메뉴만)
     * @param usrId 사용자ID
     * @return 권한 메뉴 목록
     */
    List<AuthMenuResponse> selectAuthMenuList(@Param("usrId") String usrId);

    /**
     * 사용자에게 지정한 프로그램 중 하나라도 화면 권한이 있는지 판별 (사용 중인 메뉴의 사용자 권한 또는 적용기간 내 권한그룹 권한)
     * @param usrId 사용자ID
     * @param prgmIds 프로그램ID 목록
     * @return 권한이 있으면 true
     */
    boolean existsPrgmAuth(@Param("usrId") String usrId, @Param("prgmIds") List<String> prgmIds);
}
