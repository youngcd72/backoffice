package kr.or.coder.common.frame.service;

import kr.or.coder.core.common.config.MessageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * 화면(coder-frontend)에서 사용할 다국어 메시지 전체 목록 조회 Service 구현
 * 메시지는 coder-backend 의 messages/messages_{언어}.properties 에서 일괄 관리한다.
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
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageProperties messageProperties;

    /**
     * 지정한 로케일의 메시지 전체를 키 순서로 조회 (기본 번들 위에 언어별 번들을 덮어씀)
     * @param locale 조회할 로케일 (ko, en)
     * @return 메시지 키 → 메시지 문구 맵
     */
    @Override
    public Map<String, String> getMessageList(Locale locale) {
        Map<String, String> messages = new TreeMap<>();
        String basename = messageProperties.getBasename();
        putAll(messages, basename + ".properties");
        putAll(messages, basename + "_" + locale.getLanguage() + ".properties");
        return messages;
    }

    /**
     * 클래스패스의 메시지 프로퍼티 파일을 읽어 맵에 추가 (파일이 없으면 건너뜀)
     * @param messages 메시지를 담을 맵
     * @param path 클래스패스 기준 프로퍼티 파일 경로
     * @throws UncheckedIOException 프로퍼티 파일을 읽지 못한 경우
     */
    private void putAll(Map<String, String> messages, String path) {
        Resource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            log.debug("[MessageService] Message bundle not found: {}", path);
            return;
        }
        try {
            Properties properties = PropertiesLoaderUtils.loadProperties(
                    new EncodedResource(resource, messageProperties.getEncoding()));
            properties.forEach((key, value) -> messages.put((String) key, (String) value));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load message bundle: " + path, e);
        }
    }
}
