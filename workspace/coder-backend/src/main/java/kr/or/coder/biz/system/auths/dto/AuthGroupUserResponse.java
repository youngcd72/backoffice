package kr.or.coder.biz.system.auths.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYAU030 권한그룹에 속한 사용자 응답 (TB_USR_AUTH_GRP_R + TB_USR_M)
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
public class AuthGroupUserResponse {

    /** 그룹코드 */
    private String usrGrpCd;

    /** 사용자ID */
    private String usrId;

    /** 사용자명 */
    private String usrNm;

    /** 사용자구분코드 (공통코드 SY/USR_CLS_CD) */
    private String usrClsCd;

    /** 부서코드 */
    private String deptCd;
}
