package kr.or.coder.biz.system.auths.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYAU020 권한그룹 저장 항목 (TB_AUTH_GRP_M 1건)
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
public class AuthGroupItemRequest {

    /** 그룹코드 */
    @NotBlank
    @Size(max = 20)
    private String usrGrpCd;

    /** 그룹명 */
    @NotBlank
    @Size(max = 30)
    private String usrGrpNm;

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
