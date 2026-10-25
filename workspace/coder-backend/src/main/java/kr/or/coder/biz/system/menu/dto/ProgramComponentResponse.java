package kr.or.coder.biz.system.menu.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYPG010 프로그램 컴포넌트 응답 (TB_PRGM_CMPN_M)
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
public class ProgramComponentResponse {

    /** 프로그램ID */
    private String prgmId;

    /** 컴포넌트ID (화면 버튼 등의 id) */
    private String cmpnId;

    /** 컴포넌트명 */
    private String cmpnNm;

    /** 정렬순번 */
    private Integer alinSeq;

    /** 사용여부 */
    private String useYn;
}
