package kr.or.coder.core.common.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * 현재 로케일 기준으로 다국어 메시지를 조회하는 정적 유틸
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
@Component
public class MessageUtil {

    private static MessageSource messageSource;

    /**
     * 정적 조회를 위해 MessageSource를 정적 필드에 보관
     * @param messageSource 스프링 MessageSource
     */
    public MessageUtil(MessageSource messageSource) {
        MessageUtil.messageSource = messageSource;
    }

    /**
     * 메시지 코드에 해당하는 현재 로케일의 메시지를 조회
     * @param code 메시지 코드
     * @param args 메시지 치환 인자
     * @return 치환이 완료된 메시지
     */
    public static String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
