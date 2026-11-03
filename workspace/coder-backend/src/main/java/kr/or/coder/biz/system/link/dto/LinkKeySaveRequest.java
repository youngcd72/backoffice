package kr.or.coder.biz.system.link.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYLK010 클라이언트 키 수정 요청 (TB_LNK_KEY_M)
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
public class LinkKeySaveRequest {

    /** 키상태코드 (01 사용, 02 사용중지, 03 폐기) */
    @NotBlank
    @Pattern(regexp = "^0[123]$")
    private String keyStatCd;

    /** 만료일자 (YYYYMMDD, 빈 값이면 만료 없음, 그날 23:59:59까지) */
    @Pattern(regexp = "^([0-9]{8})?$")
    private String expiDt;

    /** 허용IP주소 (IP/CIDR 쉼표 구분, 빈 값이면 제한 없음) */
    @Size(max = 1000)
    private String allwIpAddr;

    /** 분호출한도건수 (빈 값이면 제한 없음) */
    @Min(1)
    private Integer miCallLmtCnt;

    /** 일호출한도건수 (빈 값이면 제한 없음) */
    @Min(1)
    private Integer dayCallLmtCnt;

    /** 동시요청한도건수 (빈 값이면 제한 없음) */
    @Min(1)
    private Integer simuRqmtLmtCnt;

    /** 비고 */
    @Size(max = 200)
    private String rmk;
}
