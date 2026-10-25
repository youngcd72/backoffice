package kr.or.coder.biz.system.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYPG010 프로그램 저장 항목 (TB_PRGM_M 1건)
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
public class ProgramItemRequest {

    /** 프로그램ID */
    @NotBlank
    @Size(max = 100)
    private String prgmId;

    /** 프로그램명 */
    @NotBlank
    @Size(max = 50)
    private String prgmNm;

    /** 연계URL */
    @Size(max = 1000)
    private String lnkUrl;

    /** 프로그램경로코드 (공통코드 SY/PRGM_CRS_CD) */
    @Size(max = 2)
    private String prgmCrsCd;

    /** 사용여부 (Y/N) */
    @NotBlank
    @Pattern(regexp = "^[YN]$")
    private String useYn;

    /** 시작일자 (YYYYMMDD, 빈 값 허용) */
    @Pattern(regexp = "^([0-9]{8})?$")
    private String stDt;

    /** 종료일자 (YYYYMMDD, 빈 값 허용) */
    @Pattern(regexp = "^([0-9]{8})?$")
    private String endDt;

    /** 비고 */
    @Size(max = 200)
    private String rmk;
}
