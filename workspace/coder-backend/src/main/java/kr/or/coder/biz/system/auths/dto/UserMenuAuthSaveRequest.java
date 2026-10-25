package kr.or.coder.biz.system.auths.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * SYAU010 사용자 1명의 화면 권한 변경분
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
public class UserMenuAuthSaveRequest {

    /** 사용자ID */
    @NotBlank
    private String usrId;

    /** 등록(I) 목록 */
    @Valid
    private List<UserMenuAuthItemRequest> insertList = new ArrayList<>();

    /** 삭제(D) 목록 (프로그램의 화면 권한이 모두 없어지면 컴포넌트 권한도 함께 삭제) */
    @Valid
    private List<UserMenuAuthItemRequest> deleteList = new ArrayList<>();
}
