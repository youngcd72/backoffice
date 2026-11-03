package kr.or.coder.biz.system.logs.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 시스템로그에 붙일 사용자 정보 (TB_USR_M)
 * @author ywkim
 * @since 2026.10.09
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.09  ywkim        최초 생성
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
public class LogUsrResponse {

    /** 사용자ID */
    private String usrId;

    /** 사용자명 */
    private String usrNm;

    /** 사용자구분코드 */
    private String usrClsCd;

    /** 부서코드 */
    private String deptCd;
}
