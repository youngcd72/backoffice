package kr.or.coder.biz.system.menu.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYPG010 프로그램 목록 조회 조건
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
public class ProgramRequest {

    /** 프로그램경로코드 (공통코드 SY/PRGM_CRS_CD, 없으면 전체) */
    private String prgmCrsCd;

    /** 프로그램ID (부분 일치, 대소문자 무시) */
    private String prgmId;

    /** 프로그램명 (부분 일치, 대소문자 무시) */
    private String prgmNm;

    /** 사용여부 (Y/N, 없으면 전체) */
    private String useYn;
}
