package kr.or.coder.api.common.auth;

import kr.or.coder.api.common.auth.dto.TokenRequest;
import kr.or.coder.api.common.auth.dto.TokenResponse;

/**
 * 액세스 토큰 발급 Service (OAuth2 Client Credentials)
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
public interface TokenService {

    /**
     * 클라이언트 키를 검증하고 KMS 로 서명한 액세스 토큰을 발급
     * @param request 토큰 발급 요청
     * @return 액세스 토큰 응답
     * @throws OAuth2TokenException 요청이 잘못되었거나 클라이언트 인증·scope 검증에 실패한 경우
     */
    TokenResponse issueToken(TokenRequest request);
}
