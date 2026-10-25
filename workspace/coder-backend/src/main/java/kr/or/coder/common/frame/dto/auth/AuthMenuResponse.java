package kr.or.coder.common.frame.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로그인 사용자의 권한 메뉴 1건 응답 (TB_MENU_M, 권한 있는 화면 메뉴와 그 상위 메뉴)
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
@Getter
@Setter
@NoArgsConstructor
public class AuthMenuResponse {

    /** 메뉴ID */
    private String menuId;

    /** 메뉴명 */
    private String menuNm;

    /** 상위메뉴ID (최상위 메뉴는 null) */
    private String upprMenuId;

    /** 정렬순번 */
    private Integer alinSeq;

    /** 프로그램ID (화면 메뉴만, 상위 메뉴는 null) */
    private String prgmId;
}
