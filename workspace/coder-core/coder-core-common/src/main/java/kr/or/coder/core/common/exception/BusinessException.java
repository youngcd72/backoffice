package kr.or.coder.core.common.exception;

import kr.or.coder.core.common.util.MessageUtil;
import lombok.Getter;

/**
 * 업무 규칙 위반 시 발생시키는 예외 (메시지 코드로 다국어 메시지를 해석)
 * @author ywkim
 * @since 2026.09.14
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.09.14  ywkim        최초 생성
 * </pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;

    /**
     * 메시지 코드와 치환 인자로 다국어 메시지를 만들어 예외를 생성
     * @param code 메시지 코드 (messages 프로퍼티 키)
     * @param args 메시지 치환 인자
     */
    public BusinessException(String code, Object... args) {
        super(MessageUtil.getMessage(code, args));
        this.code = code;
    }

    /**
     * 메시지 코드와 원인 예외로 예외를 생성
     * @param code 메시지 코드 (messages 프로퍼티 키)
     * @param cause 원인 예외
     */
    public BusinessException(String code, Throwable cause) {
        super(MessageUtil.getMessage(code), cause);
        this.code = code;
    }
}
