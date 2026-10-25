package kr.or.coder.biz.system.auths.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * SYAU020 권한그룹·화면 권한·컴포넌트 권한 일괄 저장 요청 (행 상태 I/U/D 인 항목만 담는다)
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
public class AuthGroupSaveRequest {

    /** 등록(I) 그룹 목록 */
    @Valid
    private List<AuthGroupItemRequest> insertList = new ArrayList<>();

    /** 수정(U) 그룹 목록 */
    @Valid
    private List<AuthGroupItemRequest> updateList = new ArrayList<>();

    /** 삭제(D) 그룹 목록 (그룹 사용자·화면·컴포넌트 권한도 함께 삭제) */
    @Valid
    private List<AuthGroupItemRequest> deleteList = new ArrayList<>();

    /** 그룹별 화면 권한 변경분 (삭제 그룹 제외) */
    @Valid
    private List<AuthGroupMenuSaveRequest> menuSaveList = new ArrayList<>();

    /** 그룹·프로그램별 컴포넌트 권한 변경분 */
    @Valid
    private List<AuthGroupComponentSaveRequest> cmpnSaveList = new ArrayList<>();
}
