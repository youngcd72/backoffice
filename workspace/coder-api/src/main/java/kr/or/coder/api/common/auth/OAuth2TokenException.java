package kr.or.coder.api.common.auth;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 토큰 발급 실패 예외 (OAuth2 표준 오류 코드로 응답)
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
@Getter
public class OAuth2TokenException extends RuntimeException {

    private final String error;
    private final String description;
    private final HttpStatus status;

    /**
     * OAuth2 오류 코드와 응답 설명, 로그용 사유로 예외를 생성
     * @param error OAuth2 오류 코드
     * @param description 응답 오류 설명
     * @param reason 로그에 남길 상세 사유 (응답에는 넣지 않음)
     */
    public OAuth2TokenException(String error, String description, String reason) {
        super(reason);
        this.error = error;
        this.description = description;
        this.status = "invalid_client".equals(error) ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
    }

    /**
     * 클라이언트 인증 실패 예외를 만든다 (키 없음·사용불가·IP·비밀키 불일치·잠금을 구분하지 않는다)
     * @param reason 로그에 남길 상세 사유
     * @return invalid_client 예외
     */
    public static OAuth2TokenException invalidClient(String reason) {
        return new OAuth2TokenException("invalid_client", "Client authentication failed.", reason);
    }

    /**
     * 잘못된 요청 예외를 만든다
     * @param reason 로그와 응답에 쓸 사유
     * @return invalid_request 예외
     */
    public static OAuth2TokenException invalidRequest(String reason) {
        return new OAuth2TokenException("invalid_request", reason, reason);
    }
}
