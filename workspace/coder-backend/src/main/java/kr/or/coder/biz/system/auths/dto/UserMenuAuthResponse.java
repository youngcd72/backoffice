package kr.or.coder.biz.system.auths.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYAU010 사용자 화면 권한 응답 (TB_USR_MENU_R)
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
public class UserMenuAuthResponse {

    /** 사용자ID */
    private String usrId;

    /** 메뉴ID */
    private String menuId;

    /** 프로그램ID */
    private String prgmId;
}
