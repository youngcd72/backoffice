package kr.or.coder.biz.master.basic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MSBS010 공통코드 상세 저장 항목 (TB_COMM_CD_D 1건)
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
public class CommonCodeDetailItemRequest {

    /** 업무모듈코드 (저장 시 소속 CommonCodeDetailSaveRequest 값으로 맞춘다) */
    private String wrkMdulCd;

    /** 공통분류코드 (저장 시 소속 CommonCodeDetailSaveRequest 값으로 맞춘다) */
    private String commCtgCd;

    /** 공통분류상세코드 */
    @NotBlank
    @Size(max = 30)
    private String commCtgDtlCd;

    /** 공통분류상세명 */
    @NotBlank
    @Size(max = 80)
    private String commCtgDtlNm;

    /** 공통속성내용1 */
    @Size(max = 100)
    private String commPropCont1;

    /** 공통속성내용2 */
    @Size(max = 100)
    private String commPropCont2;

    /** 공통속성내용3 */
    @Size(max = 100)
    private String commPropCont3;

    /** 공통속성내용4 */
    @Size(max = 100)
    private String commPropCont4;

    /** 공통속성내용5 */
    @Size(max = 100)
    private String commPropCont5;

    /** 공통속성내용6 */
    @Size(max = 100)
    private String commPropCont6;

    /** 공통속성내용7 */
    @Size(max = 100)
    private String commPropCont7;

    /** 공통속성내용8 */
    @Size(max = 100)
    private String commPropCont8;

    /** 공통속성내용9 */
    @Size(max = 100)
    private String commPropCont9;

    /** 공통속성내용10 */
    @Size(max = 100)
    private String commPropCont10;

    /** 비고 */
    @Size(max = 200)
    private String rmk;

    /** 사용여부 (Y/N) */
    @NotBlank
    @Pattern(regexp = "^[YN]$")
    private String useYn;

    /** 정렬순번 */
    private Integer alinSeq;
}
