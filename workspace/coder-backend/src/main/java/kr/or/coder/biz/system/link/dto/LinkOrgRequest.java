package kr.or.coder.biz.system.link.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYLK010 연계기관 목록 조회 조건 (TB_LNK_ORG_M)
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
public class LinkOrgRequest {

    /** 기관ID·기관명 (부분 일치) */
    private String keyword;

    /** 채널구분코드 (공통코드 SY/CHNL_CLS_CD) */
    private String chnlClsCd;

    /** 사용여부 */
    private String useYn;
}
