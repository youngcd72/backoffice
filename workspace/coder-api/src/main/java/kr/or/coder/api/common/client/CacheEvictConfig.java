package kr.or.coder.api.common.client;

import kr.or.coder.api.common.config.ApiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 연계 정보 캐시 삭제 채널(coder.api.cache.evict-channel) 구독 설정.
 * 메시지 내용과 관계없이 수신하면 캐시를 모두 비운다 (Redis 가 없으면 캐시는 TTL 로만 갱신된다).
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
@Configuration
public class CacheEvictConfig {

    /**
     * 캐시 삭제 채널을 구독하는 리스너 컨테이너를 생성
     * @param connectionFactory Redis 커넥션 팩토리
     * @param linkClientCache 연계 정보 캐시
     * @param apiProperties 대외 연계 API 설정
     * @return Redis 메시지 리스너 컨테이너
     */
    @Bean
    public RedisMessageListenerContainer linkCacheEvictListenerContainer(RedisConnectionFactory connectionFactory,
                                                                         LinkClientCache linkClientCache,
                                                                         ApiProperties apiProperties) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> linkClientCache.evictAll(),
                new ChannelTopic(apiProperties.getCache().getEvictChannel()));
        return container;
    }
}
