package kr.or.coder.biz.system.link.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SYLK010 클라이언트 키 이력 응답 (TB_LNK_KEY_H)
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
public class LinkKeyHistResponse {

    /** 키이력일련번호 */
    private Long keyHistSn;

    /** 키ID */
    private String keyId;

    /** 변경 후 키상태코드 */
    private String keyStatCd;

    /** 변경내용 */
    private String chgCont;

    /** 변경자ID */
    private String firsRegrId;

    /** 변경일시 (YYYY-MM-DDTHH:mm:ss.SSS) */
    private String firsRegDt;
}
