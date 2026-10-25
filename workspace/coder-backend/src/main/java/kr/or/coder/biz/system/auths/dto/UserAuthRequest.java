package kr.or.coder.biz.system.auths.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYAU010 사용자 목록 조회 조건
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
public class UserAuthRequest {

    /** 사용자ID (부분 일치, 대소문자 무시) */
    private String usrId;

    /** 사용자명 (부분 일치) */
    private String usrNm;

    /** 사용자구분코드 (공통코드 SY/USR_CLS_CD, 없으면 전체) */
    private String usrClsCd;

    /** 사용여부 (Y/N, 없으면 전체) */
    private String useYn;
}
