package kr.or.coder.biz.system.menu.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * SYPG010 프로그램·컴포넌트 일괄 저장 요청 (행 상태 I/U/D 인 항목만 담는다)
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
public class ProgramSaveRequest {

    /** 등록(I) 프로그램 목록 */
    @Valid
    private List<ProgramItemRequest> insertList = new ArrayList<>();

    /** 수정(U) 프로그램 목록 */
    @Valid
    private List<ProgramItemRequest> updateList = new ArrayList<>();

    /** 삭제(D) 프로그램 목록 (소속 컴포넌트도 함께 삭제) */
    @Valid
    private List<ProgramItemRequest> deleteList = new ArrayList<>();

    /** 프로그램별 컴포넌트 변경분 (삭제 프로그램 제외) */
    @Valid
    private List<ProgramComponentSaveRequest> cmpnSaveList = new ArrayList<>();
}
