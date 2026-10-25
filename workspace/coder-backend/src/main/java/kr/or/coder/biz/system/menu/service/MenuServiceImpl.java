package kr.or.coder.biz.system.menu.service;

import kr.or.coder.biz.system.menu.dto.MenuItemRequest;
import kr.or.coder.biz.system.menu.dto.MenuResponse;
import kr.or.coder.biz.system.menu.dto.MenuSaveRequest;
import kr.or.coder.biz.system.menu.mapper.MenuMapper;
import kr.or.coder.common.util.RowSaveUtil;
import kr.or.coder.core.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SYPG020 메뉴관리 Service 구현
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
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuServiceImpl implements MenuService {

    /** 메뉴타입: 화면 링크 (프로그램ID 필수) */
    private static final String MENU_TYPE_LINK = "LINK";
    /** 메뉴타입: 메뉴 그룹 (프로그램ID 불가) */
    private static final String MENU_TYPE_MENU = "MENU";

    private final MenuMapper menuMapper;

    /**
     * 전체 메뉴 목록을 정렬순번 순으로 조회 (트리 구성·조건 필터링은 화면에서 처리)
     * @return 메뉴 목록
     */
    @Override
    public List<MenuResponse> getMenuList() {
        return menuMapper.selectMenuList();
    }

    /**
     * 메뉴 변경분(행 상태 I/U/D 인 항목)을 한 트랜잭션으로 저장
     * 순서: 삭제(즐겨찾기·메뉴 권한 포함) → 수정 → 등록
     * @param request 메뉴 등록/수정/삭제 목록
     * @param usrId 저장 사용자ID
     * @return 저장 건수
     * @throws BusinessException 등록 대상이 이미 있거나 수정 대상이 없는 경우, 메뉴타입과 프로그램ID가 맞지 않는 경우
     */
    @Override
    @Transactional
    public int saveMenuList(MenuSaveRequest request, String usrId) {
        int savedCnt = 0;

        request.getUpdateList().forEach(this::checkMenuTypePrgmId);
        request.getInsertList().forEach(this::checkMenuTypePrgmId);

        // 1) 삭제: FK(TB_PRGM_FAV_L → TB_MENU_M) 때문에 즐겨찾기를 먼저 지우고, 메뉴 권한도 함께 정리한다
        for (MenuItemRequest item : request.getDeleteList()) {
            menuMapper.deleteMenuFavoriteList(item.getMenuId());
            menuMapper.deleteMenuAuthGroupList(item.getMenuId());
            menuMapper.deleteMenuUserAuthList(item.getMenuId());
            savedCnt += menuMapper.deleteMenu(item.getMenuId());
        }
        // 같은 요청에서 하위 메뉴까지 지우지 않았으면 막는다 (하위 메뉴가 상위 없이 남지 않도록)
        for (MenuItemRequest item : request.getDeleteList()) {
            RowSaveUtil.checkNotExists(menuMapper.existsChildMenu(item.getMenuId()),
                    "sy.menu.save.child.exists", item.getMenuId());
        }
        // 2) 수정
        for (MenuItemRequest item : request.getUpdateList()) {
            savedCnt += RowSaveUtil.checkUpdated(menuMapper.updateMenu(item, usrId));
        }
        // 3) 등록
        for (MenuItemRequest item : request.getInsertList()) {
            RowSaveUtil.checkNotExists(menuMapper.existsMenu(item.getMenuId()),
                    "sy.menu.save.id.exists", item.getMenuId());
            savedCnt += menuMapper.insertMenu(item, usrId);
        }
        return savedCnt;
    }

    /**
     * 메뉴타입별 프로그램ID 입력 규칙을 검사 (LINK 는 프로그램ID 필수, MENU 는 프로그램ID 불가)
     * @param item 등록/수정 메뉴
     * @throws BusinessException 규칙에 맞지 않는 경우
     */
    private void checkMenuTypePrgmId(MenuItemRequest item) {
        boolean hasPrgmId = item.getPrgmId() != null && !item.getPrgmId().isBlank();
        if (MENU_TYPE_LINK.equals(item.getMenuTypeCd()) && !hasPrgmId) {
            throw new BusinessException("sy.menu.validate.prgm.id.required", item.getMenuId());
        }
        if (MENU_TYPE_MENU.equals(item.getMenuTypeCd()) && hasPrgmId) {
            throw new BusinessException("sy.menu.validate.prgm.id.not.allowed", item.getMenuId());
        }
    }
}
