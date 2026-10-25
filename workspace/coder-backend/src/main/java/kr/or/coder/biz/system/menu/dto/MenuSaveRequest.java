package kr.or.coder.biz.system.menu.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * SYPG020 메뉴 일괄 저장 요청 (행 상태 I/U/D 인 항목만 담는다)
 * @author ywkim
 * @since 2026.10.20
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.20  ywkim        최초 생성
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
public class MenuSaveRequest {

    /** 등록(I) 메뉴 목록 */
    @Valid
    private List<MenuItemRequest> insertList = new ArrayList<>();

    /** 수정(U) 메뉴 목록 */
    @Valid
    private List<MenuItemRequest> updateList = new ArrayList<>();

    /** 삭제(D) 메뉴 목록 (즐겨찾기·메뉴 권한도 함께 삭제) */
    @Valid
    private List<MenuItemRequest> deleteList = new ArrayList<>();
}
