package kr.or.coder.core.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * 다국어 메시지(MessageSource)와 Accept-Language 기반 LocaleResolver 설정
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
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MessageProperties.class)
public class MessageConfig {

    private final MessageProperties messageProperties;

    /**
     * 메시지 프로퍼티 번들을 읽는 MessageSource 빈을 생성 (키가 없으면 키 자체를 메시지로 사용)
     * @return 메시지 번들 기반 MessageSource
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames(messageProperties.getBasename());
        messageSource.setDefaultEncoding(messageProperties.getEncoding());
        messageSource.setCacheSeconds(messageProperties.getCacheSeconds());
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    /**
     * Accept-Language 헤더로 로케일을 결정하는 LocaleResolver 빈을 생성 (한국어/영어 지원)
     * @return 기본 로케일이 설정된 LocaleResolver
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setDefaultLocale(Locale.forLanguageTag(messageProperties.getDefaultLocale()));
        localeResolver.setSupportedLocales(List.of(Locale.KOREAN, Locale.ENGLISH));
        return localeResolver;
    }
}
