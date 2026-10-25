package kr.or.coder.biz.master.basic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MSBS010 공통코드 분류 저장 항목 (TB_COMM_CD_M 1건)
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
public class CommonCodeItemRequest {

    /** 업무모듈코드 */
    @NotBlank
    @Size(max = 2)
    private String wrkMdulCd;

    /** 공통분류코드 (영문 대문자·숫자·_) */
    @NotBlank
    @Size(max = 30)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    private String commCtgCd;

    /** 공통분류명 */
    @NotBlank
    @Size(max = 50)
    private String commCtgNm;

    /** 공통속성명1 */
    @Size(max = 40)
    private String commPropNm1;

    /** 공통속성명2 */
    @Size(max = 40)
    private String commPropNm2;

    /** 공통속성명3 */
    @Size(max = 40)
    private String commPropNm3;

    /** 공통속성명4 */
    @Size(max = 40)
    private String commPropNm4;

    /** 공통속성명5 */
    @Size(max = 40)
    private String commPropNm5;

    /** 공통속성명6 */
    @Size(max = 40)
    private String commPropNm6;

    /** 공통속성명7 */
    @Size(max = 40)
    private String commPropNm7;

    /** 공통속성명8 */
    @Size(max = 40)
    private String commPropNm8;

    /** 공통속성명9 */
    @Size(max = 40)
    private String commPropNm9;

    /** 공통속성명10 */
    @Size(max = 40)
    private String commPropNm10;

    /** 사용여부 (Y/N) */
    @NotBlank
    @Pattern(regexp = "^[YN]$")
    private String useYn;

    /** 사용자관리여부 (Y/N) */
    @NotBlank
    @Pattern(regexp = "^[YN]$")
    private String usrMngeYn;

    /** 정렬순번 */
    private Integer alinSeq;

    /** 비고 */
    @Size(max = 200)
    private String rmk;
}
