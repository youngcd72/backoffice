package kr.or.coder.common.frame.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로그인 사용자의 프로그램 컴포넌트 권한 1건 응답 (TB_PRGM_CMPN_M + TB_AUTH_GRP_CMPN_R + TB_USR_CMPN_R)
 * @author ywkim
 * @since 2026.10.19
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.19  ywkim        최초 생성
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
public class ComponentAuthResponse {

    /** 컴포넌트ID (화면의 ref/id 명, btnSave 등) */
    private String cmpnId;

    /** 권한여부 (소속 권한그룹 또는 사용자 권한 중 하나라도 Y 이면 Y) */
    private String authYn;
}
