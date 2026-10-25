package kr.or.coder.biz.master.basic.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * MSBS010 공통코드 분류 1건의 코드 변경분 (코드는 삭제하지 않고 사용여부로 미사용 처리)
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
public class CommonCodeDetailSaveRequest {

    /** 업무모듈코드 */
    @NotBlank
    private String wrkMdulCd;

    /** 공통분류코드 */
    @NotBlank
    private String commCtgCd;

    /** 등록(I) 코드 목록 */
    @Valid
    private List<CommonCodeDetailItemRequest> insertList = new ArrayList<>();

    /** 수정(U) 코드 목록 */
    @Valid
    private List<CommonCodeDetailItemRequest> updateList = new ArrayList<>();
}
