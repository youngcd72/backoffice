package kr.or.coder.core.kms;

/**
 * KMS 암호화 결과
 * @param ciphertext KEK 버전 정보를 포함한 암호문
 * @param kekId 암호화에 사용한 KEK 식별자
 * @param auditId KMS 감사 로그 대조용 ID (제품이 제공하지 않으면 null)
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
public record KmsEncryptResult(String ciphertext, String kekId, String auditId) {
}
