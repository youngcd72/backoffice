package kr.or.coder.common.frame.dto.favorite;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 즐겨찾기 메뉴 1건 응답 (TB_PRGM_FAV_L + TB_MENU_M)
 * @author ywkim
 * @since 2026.10.09
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.09  ywkim        최초 생성
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
public class FavoriteResponse {

    /** 메뉴ID */
    private String menuId;

    /** 메뉴명 */
    private String menuNm;

    /** 프로그램ID (메뉴에 연결된 화면) */
    private String prgmId;

    /** 정렬순번 */
    private Integer alinSeq;
}
