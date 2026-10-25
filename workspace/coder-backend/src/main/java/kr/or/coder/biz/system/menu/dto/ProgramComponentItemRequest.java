package kr.or.coder.biz.system.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYPG010 프로그램 컴포넌트 저장 항목 (TB_PRGM_CMPN_M 1건)
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
public class ProgramComponentItemRequest {

    /** 프로그램ID (저장 시 소속 ProgramComponentSaveRequest.prgmId 로 맞춘다) */
    private String prgmId;

    /** 컴포넌트ID (화면 버튼 등의 id) */
    @NotBlank
    @Size(max = 50)
    private String cmpnId;

    /** 컴포넌트명 */
    @NotBlank
    @Size(max = 50)
    private String cmpnNm;

    /** 정렬순번 */
    private Integer alinSeq;

    /** 사용여부 (Y/N) */
    @NotBlank
    @Pattern(regexp = "^[YN]$")
    private String useYn;
}
