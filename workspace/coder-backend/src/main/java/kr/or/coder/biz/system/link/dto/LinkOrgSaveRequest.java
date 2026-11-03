package kr.or.coder.biz.system.link.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYLK010 연계기관 저장 요청 (TB_LNK_ORG_M)
 * @author ywkim
 * @since 2026.11.03
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.11.03  ywkim        최초 생성
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
public class LinkOrgSaveRequest {

    /** 신규 등록 여부 */
    @NotNull
    private Boolean isNew;

    /** 연계기관ID (영문 대문자·숫자 2~20자리) */
    @NotBlank
    @Pattern(regexp = "^[A-Z0-9]{2,20}$")
    private String lnkOrgId;

    /** 연계기관명 */
    @NotBlank
    @Size(max = 100)
    private String lnkOrgNm;

    /** 채널구분코드 (공통코드 SY/CHNL_CLS_CD) */
    @NotBlank
    @Size(max = 1)
    private String chnlClsCd;

    /** 사용시작일자 (YYYYMMDD, 빈 값 허용) */
    @Pattern(regexp = "^([0-9]{8})?$")
    private String useStDt;

    /** 사용종료일자 (YYYYMMDD, 빈 값 허용) */
    @Pattern(regexp = "^([0-9]{8})?$")
    private String useEndDt;

    /** 사용여부 (Y/N) */
    @NotBlank
    @Pattern(regexp = "^[YN]$")
    private String useYn;

    /** 비고 */
    @Size(max = 200)
    private String rmk;
}
