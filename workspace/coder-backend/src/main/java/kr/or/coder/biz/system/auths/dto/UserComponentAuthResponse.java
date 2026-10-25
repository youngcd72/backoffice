package kr.or.coder.biz.system.auths.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYAU010 사용자 컴포넌트 권한 응답 (TB_USR_CMPN_R)
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
public class UserComponentAuthResponse {

    /** 사용자ID */
    private String usrId;

    /** 프로그램ID */
    private String prgmId;

    /** 컴포넌트ID */
    private String cmpnId;

    /** 권한여부 */
    private String authYn;
}
