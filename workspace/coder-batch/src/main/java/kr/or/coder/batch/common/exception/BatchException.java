package kr.or.coder.batch.common.exception;

import kr.or.coder.core.common.exception.BusinessException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * HTTP 상태코드를 갖는 배치 업무 예외.
 * core의 GlobalExceptionHandler는 BusinessException을 모두 400으로 응답하므로,
 * BatchExceptionHandler가 먼저 처리해 404/409 등을 돌려준다.
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
@Getter
public class BatchException extends BusinessException {

    private final HttpStatus status;

    /**
     * 오류코드에 해당하는 메시지 키와 HTTP 상태로 예외를 생성한다.
     * @param errorCode 배치 오류코드
     * @param args 메시지 치환 인자
     */
    public BatchException(BatchErrorCode errorCode, Object... args) {
        super(errorCode.getMessageKey(), args);
        this.status = errorCode.getStatus();
    }
}
