package kr.or.coder.core.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 다국어 메시지 설정(coder.message.*) 프로퍼티
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
@Setter
@ConfigurationProperties(prefix = "coder.message")
public class MessageProperties {

    private String basename = "messages/messages";
    private String encoding = "UTF-8";
    private String defaultLocale = "ko";
    private int cacheSeconds = 3600;
}
