package kr.or.coder.biz.system.menu.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYPG020 메뉴 응답 (TB_MENU_M)
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
public class MenuResponse {

    /** 메뉴ID */
    private String menuId;

    /** 메뉴명 */
    private String menuNm;

    /** 상위메뉴ID (최상위는 빈 문자열) */
    private String upprMenuId;

    /** 정렬순번 */
    private Integer alinSeq;

    /** 메뉴타입코드 (공통코드 SY/MENU_TYPE_CD: MENU 메뉴, LINK 링크) */
    private String menuTypeCd;

    /** 사용여부 */
    private String useYn;

    /** 프로그램ID (링크 메뉴만) */
    private String prgmId;

    /** 비고 */
    private String rmk;
}
