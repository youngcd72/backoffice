package kr.or.coder.biz.system.link.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYLK010 인터페이스 저장 요청 (TB_INFC_M)
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
public class InterfaceSaveRequest {

    /** 신규 등록 여부 */
    @NotNull
    private Boolean isNew;

    /** 인터페이스ID (IF + 업무약어 2자리 + 순번 4자리) */
    @NotBlank
    @Pattern(regexp = "^IF[A-Z]{2}[0-9]{4}$")
    private String infcId;

    /** 인터페이스명 */
    @NotBlank
    @Size(max = 100)
    private String infcNm;

    /** HTTP메서드명 */
    @NotBlank
    @Pattern(regexp = "^(GET|POST|PUT|PATCH|DELETE)$")
    private String httpMethNm;

    /** URL (/api/v + 버전 + / 로 시작, 공백 없음) */
    @NotBlank
    @Size(max = 200)
    @Pattern(regexp = "^/api/v[0-9]+/\\S*$")
    private String url;

    /** 본문로그여부 (Y/N) */
    @NotBlank
    @Pattern(regexp = "^[YN]$")
    private String bodyLogYn;

    /** 마스킹필드내용 (쉼표 구분) */
    @Size(max = 1000)
    private String maskFldCont;

    /** 사용여부 (Y/N) */
    @NotBlank
    @Pattern(regexp = "^[YN]$")
    private String useYn;

    /** 비고 */
    @Size(max = 200)
    private String rmk;
}
