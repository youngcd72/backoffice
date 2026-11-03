package kr.or.coder.biz.system.link.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * coder-api 연계 정보 캐시 삭제 알림 발행 (Redis 채널 coder:api:cache-evict)
 * coder-api 는 메시지 내용과 관계없이 연계키·인터페이스·허용 인터페이스 캐시를 모두 비운다.
 * @author ywkim
 * @since 2026.11.03
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.11.03  ywkim        최초 생성
 * </pre>
 */
@Slf4j
@Component
public class LinkCacheEvictPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final String channel;

    /**
     * Redis 템플릿과 발행 채널로 생성
     * @param stringRedisTemplate Redis 문자열 템플릿
     * @param channel 캐시 삭제 채널 (coder.link.cache-evict-channel, coder-api 설정과 같아야 함)
     */
    public LinkCacheEvictPublisher(StringRedisTemplate stringRedisTemplate,
                                   @Value("${coder.link.cache-evict-channel:coder:api:cache-evict}") String channel) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.channel = channel;
    }

    /**
     * 캐시 삭제 알림을 발행 (진행 중인 트랜잭션이 있으면 커밋 후 발행)
     * @param target 변경 대상 (로그 확인용, 예: ORG:LOCAL01)
     */
    public void publish(String target) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishNow(target);
                }
            });
        } else {
            publishNow(target);
        }
    }

    /**
     * Redis 채널로 발행. 실패해도 저장은 끝났으므로 WARN 만 남긴다 (coder-api 캐시는 최대 TTL 뒤 갱신)
     * @param target 변경 대상
     */
    private void publishNow(String target) {
        try {
            stringRedisTemplate.convertAndSend(channel, target);
        } catch (RuntimeException e) {
            log.warn("coder-api 캐시 삭제 알림 발행 실패 target={} cause={}", target, e.getMessage());
        }
    }
}
