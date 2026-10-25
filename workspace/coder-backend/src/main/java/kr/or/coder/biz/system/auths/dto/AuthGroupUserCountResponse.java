package kr.or.coder.biz.system.auths.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYAU030 권한그룹 + 그룹별 사용자수 응답
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
public class AuthGroupUserCountResponse {

    /** 그룹코드 */
    private String usrGrpCd;

    /** 그룹명 */
    private String usrGrpNm;

    /** 사용여부 */
    private String useYn;

    /** 그룹 사용자수 */
    private Integer usrCnt;
}
