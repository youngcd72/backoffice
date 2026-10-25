package kr.or.coder.common.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;

/**
 * SSE 설정: heartbeat 스케줄링과 Redis 채널(coder:sse) 구독
 * 어느 인스턴스에서 발행하든 모든 인스턴스가 받아서 자기에게 접속한 사용자에게만 전송한다.
 * @author ywkim
 * @since 2026.10.17
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.17  ywkim        최초 생성
 * </pre>
 */
@Slf4j
@Configuration
@EnableScheduling
public class SseConfig {

    /**
     * Redis SSE 채널을 구독해 받은 메시지를 이 인스턴스의 사용자 연결로 전송하는 리스너 컨테이너를 생성
     * @param connectionFactory Redis 커넥션 팩토리
     * @param objectMapper 메시지 역직렬화용 ObjectMapper
     * @param sseEmitterRegistry SSE 연결 저장소
     * @return Redis 메시지 리스너 컨테이너
     */
    @Bean
    public RedisMessageListenerContainer sseRedisMessageListenerContainer(RedisConnectionFactory connectionFactory,
                                                                          ObjectMapper objectMapper,
                                                                          SseEmitterRegistry sseEmitterRegistry) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            try {
                SseMessage sseMessage = objectMapper.readValue(message.getBody(), SseMessage.class);
                sseEmitterRegistry.send(sseMessage.usrId(), sseMessage.eventName(), sseMessage.data());
            } catch (IOException e) {
                log.error("SSE 메시지 역직렬화 실패", e);
            }
        }, new ChannelTopic(SsePublisher.SSE_CHANNEL));
        return container;
    }
}
