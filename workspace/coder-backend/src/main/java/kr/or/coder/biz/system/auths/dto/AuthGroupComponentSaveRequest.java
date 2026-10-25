package kr.or.coder.biz.system.auths.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * SYAU020 권한그룹·프로그램 1건의 컴포넌트 권한 변경분
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
public class AuthGroupComponentSaveRequest {

    /** 그룹코드 */
    @NotBlank
    private String usrGrpCd;

    /** 프로그램ID */
    @NotBlank
    private String prgmId;

    /** 등록(I) 목록 */
    @Valid
    private List<AuthGroupComponentItemRequest> insertList = new ArrayList<>();

    /** 수정(U) 목록 */
    @Valid
    private List<AuthGroupComponentItemRequest> updateList = new ArrayList<>();
}
