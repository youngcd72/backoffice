package kr.or.coder.biz.system.auths.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYAU010 사용자 화면 권한 저장 항목 (TB_USR_MENU_R 1건)
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
public class UserMenuAuthItemRequest {

    /** 사용자ID (저장 시 소속 UserMenuAuthSaveRequest 값으로 맞춘다) */
    private String usrId;

    /** 메뉴ID */
    @NotBlank
    @Size(max = 20)
    private String menuId;

    /** 프로그램ID */
    @NotBlank
    @Size(max = 100)
    private String prgmId;
}
