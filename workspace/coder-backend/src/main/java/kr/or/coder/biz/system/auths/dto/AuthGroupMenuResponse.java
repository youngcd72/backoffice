package kr.or.coder.biz.system.auths.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYAU020 권한그룹 화면 권한 응답 (TB_AUTH_GRP_MENU_R)
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
public class AuthGroupMenuResponse {

    /** 그룹코드 */
    private String usrGrpCd;

    /** 메뉴ID */
    private String menuId;

    /** 프로그램ID */
    private String prgmId;
}
