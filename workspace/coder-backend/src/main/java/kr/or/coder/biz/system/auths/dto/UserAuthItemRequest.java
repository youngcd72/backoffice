package kr.or.coder.biz.system.auths.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYAU010 사용자 저장 항목 (TB_USR_M 1건)
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
public class UserAuthItemRequest {

    /** 사용자ID */
    @NotBlank
    @Size(max = 10)
    private String usrId;

    /** 사용자명 */
    @NotBlank
    @Size(max = 50)
    private String usrNm;

    /** 사원번호 */
    @Size(max = 10)
    private String empNo;

    /** 부서코드 */
    @Size(max = 10)
    private String deptCd;

    /** 이메일주소 */
    @Size(max = 500)
    private String emlAddr;

    /** 모바일번호 */
    @Size(max = 64)
    private String moblNo;

    /** 사용자상태코드 (재직여부) */
    @Size(max = 1)
    private String usrStatCd;

    /** 사용자잠금여부 (Y/N) */
    @Pattern(regexp = "^[YN]?$")
    private String usrLockYn;

    /** 사용자구분코드 (공통코드 SY/USR_CLS_CD) */
    @Size(max = 1)
    private String usrClsCd;

    /** 입사일자 (YYYYMMDD, 빈 값 허용) */
    @Pattern(regexp = "^([0-9]{8})?$")
    private String ecomDt;

    /** 퇴사일자 (YYYYMMDD, 빈 값 허용) */
    @Pattern(regexp = "^([0-9]{8})?$")
    private String resiDt;

    /** 사용시작일자 (YYYYMMDD) */
    @Pattern(regexp = "^([0-9]{8})?$")
    private String stDt;

    /** 사용종료일자 (YYYYMMDD) */
    @Pattern(regexp = "^([0-9]{8})?$")
    private String endDt;

    /** 사용여부 (Y/N) */
    @NotBlank
    @Pattern(regexp = "^[YN]$")
    private String useYn;
}
