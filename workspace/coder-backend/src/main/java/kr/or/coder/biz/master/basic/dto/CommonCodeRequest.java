package kr.or.coder.biz.master.basic.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MSBS010 공통코드 분류 목록 조회 조건
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
public class CommonCodeRequest {

    /** 업무모듈코드 (일치, 없으면 전체) */
    private String wrkMdulCd;

    /** 공통분류코드 (부분 일치, 대소문자 무시) */
    private String commCtgCd;

    /** 공통분류명 (부분 일치) */
    private String commCtgNm;
}
