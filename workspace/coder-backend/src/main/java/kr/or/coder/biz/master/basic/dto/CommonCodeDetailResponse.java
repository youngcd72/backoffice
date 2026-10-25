package kr.or.coder.biz.master.basic.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 공통코드 상세 응답 (TB_COMM_CD_D, 공통코드관리 코드목록 및 화면 콤보용)
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
public class CommonCodeDetailResponse {

    /** 업무모듈코드 */
    private String wrkMdulCd;

    /** 공통분류코드 */
    private String commCtgCd;

    /** 공통분류상세코드 */
    private String commCtgDtlCd;

    /** 공통분류상세명 */
    private String commCtgDtlNm;

    /** 공통속성내용1 */
    private String commPropCont1;

    /** 공통속성내용2 */
    private String commPropCont2;

    /** 공통속성내용3 */
    private String commPropCont3;

    /** 공통속성내용4 */
    private String commPropCont4;

    /** 공통속성내용5 */
    private String commPropCont5;

    /** 공통속성내용6 */
    private String commPropCont6;

    /** 공통속성내용7 */
    private String commPropCont7;

    /** 공통속성내용8 */
    private String commPropCont8;

    /** 공통속성내용9 */
    private String commPropCont9;

    /** 공통속성내용10 */
    private String commPropCont10;

    /** 비고 */
    private String rmk;

    /** 사용여부 */
    private String useYn;

    /** 정렬순번 */
    private Integer alinSeq;
}
