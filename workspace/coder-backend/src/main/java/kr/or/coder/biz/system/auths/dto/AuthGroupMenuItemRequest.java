package kr.or.coder.biz.system.auths.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYAU020 권한그룹 화면 권한 저장 항목 (TB_AUTH_GRP_MENU_R 1건)
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
@Getter
@Setter
@NoArgsConstructor
public class AuthGroupMenuItemRequest {

    /** 그룹코드 (저장 시 소속 AuthGroupMenuSaveRequest 값으로 맞춘다) */
    private String usrGrpCd;

    /** 메뉴ID */
    @NotBlank
    @Size(max = 20)
    private String menuId;

    /** 프로그램ID */
    @NotBlank
    @Size(max = 100)
    private String prgmId;
}
