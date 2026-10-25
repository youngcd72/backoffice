package kr.or.coder.biz.master.basic.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MSBS010 공통코드 분류 응답 (TB_COMM_CD_M)
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
public class CommonCodeResponse {

    /** 업무모듈코드 */
    private String wrkMdulCd;

    /** 공통분류코드 */
    private String commCtgCd;

    /** 공통분류명 */
    private String commCtgNm;

    /** 공통속성명1 */
    private String commPropNm1;

    /** 공통속성명2 */
    private String commPropNm2;

    /** 공통속성명3 */
    private String commPropNm3;

    /** 공통속성명4 */
    private String commPropNm4;

    /** 공통속성명5 */
    private String commPropNm5;

    /** 공통속성명6 */
    private String commPropNm6;

    /** 공통속성명7 */
    private String commPropNm7;

    /** 공통속성명8 */
    private String commPropNm8;

    /** 공통속성명9 */
    private String commPropNm9;

    /** 공통속성명10 */
    private String commPropNm10;

    /** 사용여부 */
    private String useYn;

    /** 사용자관리여부 (Y: 화면에서 코드를 관리) */
    private String usrMngeYn;

    /** 정렬순번 */
    private Integer alinSeq;

    /** 비고 */
    private String rmk;
}
