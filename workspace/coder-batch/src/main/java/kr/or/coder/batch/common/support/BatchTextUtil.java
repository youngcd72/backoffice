package kr.or.coder.batch.common.support;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.core.NestedExceptionUtils;

import java.nio.charset.StandardCharsets;

/**
 * 배치 문자열 처리 유틸리티 (byte 길이 제한, 오류메시지 포맷, 사용자ID 길이 제한)
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
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BatchTextUtil {

    /**
     * UTF-8 기준 maxBytes 이내로 문자열을 자른다 (멀티바이트 문자 중간에서 자르지 않음).
     * @param text 원본 문자열
     * @param maxBytes 최대 byte 수
     * @return 잘라낸 문자열, text가 null이면 null
     */
    public static String truncateBytes(String text, int maxBytes) {
        if (text == null) {
            return null;
        }
        if (text.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        int bytes = 0;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            int charBytes = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8).length;
            if (bytes + charBytes > maxBytes) {
                break;
            }
            sb.appendCodePoint(codePoint);
            bytes += charBytes;
            i += Character.charCount(codePoint);
        }
        return sb.toString();
    }

    /**
     * 최상위 예외 메시지와 root cause 메시지를 합쳐 오류메시지내용(ERR_MSG_CONT)을 만든다.
     * @param throwable 대상 예외
     * @return 컬럼 길이에 맞춰 자른 오류메시지, throwable이 null이면 null
     */
    public static String formatErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        String message = throwable.getClass().getName() + ": " + throwable.getMessage();
        Throwable rootCause = NestedExceptionUtils.getRootCause(throwable);
        if (rootCause != null && rootCause != throwable) {
            message += " / Caused by " + rootCause.getClass().getName() + ": " + rootCause.getMessage();
        }
        return truncateBytes(message, BatchConstants.ERR_MSG_MAX_BYTES);
    }

    /**
     * 사용자ID 컬럼(VARCHAR(10)) 길이에 맞춘다.
     * @param userId 사용자 ID
     * @return 최대 길이로 자른 사용자 ID, null이면 null
     */
    public static String limitUserId(String userId) {
        if (userId == null || userId.length() <= BatchConstants.USR_ID_MAX_LENGTH) {
            return userId;
        }
        return userId.substring(0, BatchConstants.USR_ID_MAX_LENGTH);
    }
}
