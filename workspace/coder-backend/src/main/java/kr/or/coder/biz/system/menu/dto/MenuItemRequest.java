package kr.or.coder.biz.system.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYPG020 메뉴 저장 항목 (TB_MENU_M 1건)
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
public class MenuItemRequest {

    /** 메뉴ID */
    @NotBlank
    @Size(max = 20)
    private String menuId;

    /** 메뉴명 */
    @NotBlank
    @Size(max = 50)
    private String menuNm;

    /** 상위메뉴ID (최상위는 빈 문자열) */
    @Size(max = 20)
    private String upprMenuId;

    /** 정렬순번 */
    @NotNull
    private Integer alinSeq;

    /** 메뉴타입코드 (공통코드 SY/MENU_TYPE_CD: MENU 메뉴, LINK 링크) */
    @Size(max = 4)
    private String menuTypeCd;

    /** 사용여부 (Y/N) */
    @NotBlank
    @Pattern(regexp = "^[YN]$")
    private String useYn;

    /** 프로그램ID (링크 메뉴만) */
    @Size(max = 100)
    private String prgmId;

    /** 비고 */
    @Size(max = 200)
    private String rmk;
}
