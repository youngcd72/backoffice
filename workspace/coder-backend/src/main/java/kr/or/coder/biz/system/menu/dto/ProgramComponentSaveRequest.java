package kr.or.coder.biz.system.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * SYPG010 프로그램 1건의 컴포넌트 변경분 (행 상태 I/U/D 별 목록)
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
public class ProgramComponentSaveRequest {

    /** 프로그램ID */
    @NotBlank
    private String prgmId;

    /** 등록(I) 목록 */
    @Valid
    private List<ProgramComponentItemRequest> insertList = new ArrayList<>();

    /** 수정(U) 목록 */
    @Valid
    private List<ProgramComponentItemRequest> updateList = new ArrayList<>();

    /** 삭제(D) 목록 */
    @Valid
    private List<ProgramComponentItemRequest> deleteList = new ArrayList<>();
}
