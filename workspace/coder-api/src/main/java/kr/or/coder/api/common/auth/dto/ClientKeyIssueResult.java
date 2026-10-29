package kr.or.coder.api.common.auth.dto;

import java.time.LocalDateTime;

/**
 * 클라이언트 키 발급 결과 (비밀키 원문은 이 결과에서 한 번만 전달하고 저장·로그에 남기지 않는다)
 * @param keyId 키ID (client_id)
 * @param clientSecret 비밀키 원문 (client_secret)
 * @param expiDttm 만료일시 (없으면 만료 없음)
 * @author ywkim
 * @since 2026.10.27
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.27  ywkim        최초 생성
 * </pre>
 */
public record ClientKeyIssueResult(String keyId, String clientSecret, LocalDateTime expiDttm) {
}
