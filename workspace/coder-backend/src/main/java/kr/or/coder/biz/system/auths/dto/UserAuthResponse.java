package kr.or.coder.biz.system.auths.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYAU010 사용자 응답 (TB_USR_M)
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
public class UserAuthResponse {

    /** 사용자ID */
    private String usrId;

    /** 사용자명 */
    private String usrNm;

    /** 사원번호 */
    private String empNo;

    /** 부서코드 */
    private String deptCd;

    /** 이메일주소 */
    private String emlAddr;

    /** 모바일번호 */
    private String moblNo;

    /** 사용자상태코드 (재직여부) */
    private String usrStatCd;

    /** 사용자잠금여부 */
    private String usrLockYn;

    /** 사용자구분코드 (공통코드 SY/USR_CLS_CD) */
    private String usrClsCd;

    /** 입사일자 (YYYYMMDD) */
    private String ecomDt;

    /** 퇴사일자 (YYYYMMDD) */
    private String resiDt;

    /** 사용시작일자 (YYYYMMDD) */
    private String stDt;

    /** 사용종료일자 (YYYYMMDD) */
    private String endDt;

    /** 사용여부 */
    private String useYn;
}
