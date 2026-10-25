package kr.or.coder.biz.system.auths.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYAU020/SYAU030 권한그룹 목록 조회 조건
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
public class AuthGroupRequest {

    /** 그룹코드 (부분 일치, 대소문자 무시) */
    private String usrGrpCd;

    /** 그룹명 (부분 일치) */
    private String usrGrpNm;

    /** 사용여부 (Y/N, 없으면 전체) */
    private String useYn;
}
