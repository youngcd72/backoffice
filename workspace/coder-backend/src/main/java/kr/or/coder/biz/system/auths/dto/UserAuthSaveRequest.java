package kr.or.coder.biz.system.auths.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * SYAU010 사용자·화면 권한·컴포넌트 권한 일괄 저장 요청 (행 상태 I/U/D 인 항목만 담는다)
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
public class UserAuthSaveRequest {

    /** 등록(I) 사용자 목록 */
    @Valid
    private List<UserAuthItemRequest> insertList = new ArrayList<>();

    /** 수정(U) 사용자 목록 */
    @Valid
    private List<UserAuthItemRequest> updateList = new ArrayList<>();

    /** 삭제(D) 사용자 목록 (권한그룹·즐겨찾기·화면·컴포넌트 권한도 함께 삭제) */
    @Valid
    private List<UserAuthItemRequest> deleteList = new ArrayList<>();

    /** 사용자별 화면 권한 변경분 (삭제 사용자 제외) */
    @Valid
    private List<UserMenuAuthSaveRequest> menuAuthSaveList = new ArrayList<>();

    /** 사용자·프로그램별 컴포넌트 권한 변경분 */
    @Valid
    private List<UserComponentAuthSaveRequest> cmpnAuthSaveList = new ArrayList<>();
}
