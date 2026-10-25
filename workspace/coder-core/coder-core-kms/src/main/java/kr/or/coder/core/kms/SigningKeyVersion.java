package kr.or.coder.core.kms;

import java.security.PublicKey;
import java.time.Instant;

/**
 * 서명키 버전 메타데이터
 * @param signingKeyId 서명키 ID
 * @param version 버전 (KMS 고유값)
 * @param kid JWT 헤더 kid / JWKS 키 식별자
 * @param publicKey 공개키 (JWKS 게시용)
 * @param createdAt 버전 생성 시각
 * @param auditId KMS 감사 로그 대조용 ID (없으면 null)
 * @author ywkim
 * @since 2026.10.09
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.09  ywkim        최초 생성
 * </pre>
 */
public record SigningKeyVersion(String signingKeyId,
                                String version,
                                String kid,
                                PublicKey publicKey,
                                Instant createdAt,
                                String auditId) {
}
