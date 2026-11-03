package kr.or.coder.biz.system.link.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYLK010 인터페이스 목록 조회 조건 (TB_INFC_M)
 * @author ywkim
 * @since 2026.11.03
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.11.03  ywkim        최초 생성
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
public class InterfaceRequest {

    /** 인터페이스ID·명·URL (부분 일치) */
    private String keyword;

    /** 사용여부 */
    private String useYn;
}
