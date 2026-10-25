package kr.or.coder.biz.system.menu.mapper;

import kr.or.coder.biz.system.menu.dto.MenuItemRequest;
import kr.or.coder.biz.system.menu.dto.MenuResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SYPG020 메뉴관리 MyBatis Mapper (TB_MENU_M)
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
public interface MenuMapper {

    /**
     * 전체 메뉴 목록을 정렬순번 순으로 조회 (트리 구성·조건 필터링은 화면에서 처리)
     * @return 메뉴 목록
     */
    List<MenuResponse> selectMenuList();

    /**
     * 메뉴ID가 등록되어 있는지 판별
     * @param menuId 메뉴ID
     * @return 등록되어 있으면 true
     */
    boolean existsMenu(@Param("menuId") String menuId);

    /**
     * 메뉴를 등록
     * @param item 메뉴 1건
     * @param usrId 등록/수정 사용자ID
     * @return 등록 건수
     */
    int insertMenu(@Param("item") MenuItemRequest item, @Param("usrId") String usrId);

    /**
     * 메뉴를 수정
     * @param item 메뉴 1건
     * @param usrId 등록/수정 사용자ID
     * @return 수정 건수
     */
    int updateMenu(@Param("item") MenuItemRequest item, @Param("usrId") String usrId);

    /**
     * 메뉴를 삭제
     * @param menuId 메뉴ID
     * @return 삭제 건수
     */
    int deleteMenu(@Param("menuId") String menuId);

    /**
     * 메뉴의 즐겨찾기를 모두 삭제 (메뉴 삭제 시)
     * @param menuId 메뉴ID
     * @return 삭제 건수
     */
    int deleteMenuFavoriteList(@Param("menuId") String menuId);

    /**
     * 메뉴의 권한그룹 메뉴 권한을 모두 삭제 (메뉴 삭제 시)
     * @param menuId 메뉴ID
     * @return 삭제 건수
     */
    int deleteMenuAuthGroupList(@Param("menuId") String menuId);

    /**
     * 메뉴의 사용자 메뉴 권한을 모두 삭제 (메뉴 삭제 시)
     * @param menuId 메뉴ID
     * @return 삭제 건수
     */
    int deleteMenuUserAuthList(@Param("menuId") String menuId);

    /**
     * 하위 메뉴가 있는지 판별
     * @param menuId 메뉴ID
     * @return 하위 메뉴가 있으면 true
     */
    boolean existsChildMenu(@Param("menuId") String menuId);
}
