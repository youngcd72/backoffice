package kr.or.coder.biz.system.link.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SYLK010 클라이언트 키 발급 결과 (비밀키 원문은 이 응답에만 담는다)
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
@AllArgsConstructor
public class LinkKeyIssueResponse {

    /** 키ID (OAuth2 client_id) */
    private String keyId;

    /** 비밀키 원문 (OAuth2 client_secret, 저장하지 않음) */
    private String clientSecret;

    /** 만료일시 (YYYY-MM-DDTHH:mm:ss.SSS, 없으면 만료 없음) */
    private String expiDttm;
}
