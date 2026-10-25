package kr.or.coder.common.frame.dto.auth;

import lombok.Getter;
import lombok.Setter;

/**
 * 로그인 검증용 사용자 정보 (비밀번호 해시와 계정 상태 포함, 응답에 내보내지 않는다)
 * @author ywkim
 * @since 2026.10.20
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.20  ywkim        최초 생성
 * </pre>
 */
@Getter
@Setter
public class LoginUser {

    /** 사용자ID */
    private String usrId;
    /** 사용자명 */
    private String usrNm;
    /** 이메일주소 */
    private String emlAddr;
    /** 역할 (ADMIN: 관리자 권한그룹 소속, USER: 그 외) */
    private String role;
    /** 비밀번호 (BCrypt 해시) */
    private String pwd;
    /** 사용여부 */
    private String useYn;
    /** 사용자잠금여부 */
    private String usrLockYn;
    /** 로그인오류건수 */
    private Integer lginErrCnt;
    /** 사용시작일자 (YYYYMMDD) */
    private String useStDt;
    /** 사용종료일자 (YYYYMMDD) */
    private String useEndDt;
}
