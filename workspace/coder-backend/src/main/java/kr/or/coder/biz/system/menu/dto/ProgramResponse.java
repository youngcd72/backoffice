package kr.or.coder.biz.system.menu.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYPG010 프로그램 목록 응답 (TB_PRGM_M)
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
public class ProgramResponse {

    /** 프로그램ID */
    private String prgmId;

    /** 프로그램명 */
    private String prgmNm;

    /** 연계URL */
    private String lnkUrl;

    /** 프로그램경로코드 (공통코드 SY/PRGM_CRS_CD) */
    private String prgmCrsCd;

    /** 사용여부 */
    private String useYn;

    /** 시작일자 (YYYYMMDD) */
    private String stDt;

    /** 종료일자 (YYYYMMDD) */
    private String endDt;

    /** 비고 */
    private String rmk;
}
