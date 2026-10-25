package kr.or.coder.biz.system.auths.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYAU020 권한그룹 응답 (TB_AUTH_GRP_M)
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
public class AuthGroupResponse {

    /** 그룹코드 */
    private String usrGrpCd;

    /** 그룹명 */
    private String usrGrpNm;

    /** 사용여부 */
    private String useYn;

    /** 시작일자 (YYYYMMDD) */
    private String stDt;

    /** 종료일자 (YYYYMMDD) */
    private String endDt;

    /** 비고 */
    private String rmk;
}
