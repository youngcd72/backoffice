package kr.or.coder.biz.system.menu.service;

import kr.or.coder.biz.system.menu.dto.MenuResponse;
import kr.or.coder.biz.system.menu.dto.MenuSaveRequest;

import java.util.List;

/**
 * SYPG020 메뉴관리 Service
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
public interface MenuService {

    /**
     * 전체 메뉴 목록을 정렬순번 순으로 조회 (트리 구성·조건 필터링은 화면에서 처리)
     * @return 메뉴 목록
     */
    List<MenuResponse> getMenuList();

    /**
     * 메뉴 변경분(행 상태 I/U/D 인 항목)을 한 트랜잭션으로 저장
     * @param request 메뉴 등록/수정/삭제 목록
     * @param usrId 저장 사용자ID
     * @return 저장 건수
     */
    int saveMenuList(MenuSaveRequest request, String usrId);
}
