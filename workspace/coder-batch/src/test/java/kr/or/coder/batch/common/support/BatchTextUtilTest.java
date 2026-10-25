package kr.or.coder.batch.common.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BatchTextUtil 단위 테스트
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
class BatchTextUtilTest {

    /**
     * UTF-8 byte 기준으로 자르고 한글 문자 중간에서 자르지 않는지 검증한다.
     */
    @Test
    @DisplayName("UTF-8 byte 기준으로 자르고 한글 문자 중간에서 자르지 않는다")
    void truncateBytes() {
        // 한글 1자 = 3byte
        assertThat(BatchTextUtil.truncateBytes("가나다", 7)).isEqualTo("가나");
        assertThat(BatchTextUtil.truncateBytes("abc", 3)).isEqualTo("abc");
        assertThat(BatchTextUtil.truncateBytes(null, 3)).isNull();

        String longText = "오류".repeat(1000);
        String truncated = BatchTextUtil.truncateBytes(longText, BatchConstants.ERR_MSG_MAX_BYTES);
        assertThat(truncated.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(BatchConstants.ERR_MSG_MAX_BYTES);
    }

    /**
     * 오류메시지에 최상위 예외와 root cause가 함께 담기는지 검증한다.
     */
    @Test
    @DisplayName("오류메시지에 최상위 예외와 root cause를 함께 담는다")
    void formatErrorMessage() {
        Exception exception = new IllegalStateException("step failed", new IllegalArgumentException("bad row"));

        String message = BatchTextUtil.formatErrorMessage(exception);

        assertThat(message)
                .contains("IllegalStateException: step failed")
                .contains("Caused by java.lang.IllegalArgumentException: bad row");
    }

    /**
     * 사용자ID가 10자로 제한되는지 검증한다.
     */
    @Test
    @DisplayName("사용자ID는 10자로 제한한다")
    void limitUserId() {
        assertThat(BatchTextUtil.limitUserId("administrator01")).isEqualTo("administra");
        assertThat(BatchTextUtil.limitUserId("admin")).isEqualTo("admin");
    }
}
