package kr.or.coder.biz.system.auths.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * SYAU030 권한그룹 1건의 사용자 추가/제외 요청
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
public class AuthGroupUserSaveRequest {

    /** 그룹에 추가할 사용자ID 목록 */
    private List<String> insertList = new ArrayList<>();

    /** 그룹에서 제외할 사용자ID 목록 */
    private List<String> deleteList = new ArrayList<>();
}
