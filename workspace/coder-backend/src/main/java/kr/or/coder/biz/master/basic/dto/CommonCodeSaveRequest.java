package kr.or.coder.biz.master.basic.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * MSBS010 공통코드 분류·코드 일괄 저장 요청 (행 상태 I/U/D 인 항목만 담는다)
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
public class CommonCodeSaveRequest {

    /** 등록(I) 분류 목록 */
    @Valid
    private List<CommonCodeItemRequest> insertList = new ArrayList<>();

    /** 수정(U) 분류 목록 */
    @Valid
    private List<CommonCodeItemRequest> updateList = new ArrayList<>();

    /** 삭제(D) 분류 목록 (코드가 등록되지 않은 분류만 삭제할 수 있다) */
    @Valid
    private List<CommonCodeItemRequest> deleteList = new ArrayList<>();

    /** 분류별 코드 변경분 (삭제 분류 제외) */
    @Valid
    private List<CommonCodeDetailSaveRequest> dtlSaveList = new ArrayList<>();
}
