package kr.or.coder.common.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 사용자에게 SSE 이벤트를 발행 (Redis 채널로 발행해 사용자가 접속한 인스턴스가 전송)
 * 트랜잭션 안에서 호출하면 커밋된 뒤에 발행한다 (롤백된 알람이 화면에 표시되지 않도록).
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
@Component
@RequiredArgsConstructor
public class SsePublisher {

    /** SSE 메시지를 중계하는 Redis 채널 (coder-batch 등 다른 프로세스도 이 채널로 발행할 수 있다) */
    public static final String SSE_CHANNEL = "coder:sse";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SseEmitterRegistry sseEmitterRegistry;

    /**
     * 사용자에게 SSE 이벤트를 발행 (진행 중인 트랜잭션이 있으면 커밋 후 발행)
     * @param usrId 수신 사용자ID
     * @param eventName 이벤트명
     * @param data 이벤트 데이터
     */
    public void publish(String usrId, String eventName, Object data) {
        SseMessage message = new SseMessage(usrId, eventName, data);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishNow(message);
                }
            });
        } else {
            publishNow(message);
        }
    }

    /**
     * Redis 채널로 메시지를 발행하고, Redis 를 쓸 수 없으면 이 인스턴스의 연결에만 직접 전송
     * 알람은 DB 에 저장되어 있으므로 실시간 전송이 실패해도 다음 조회 때 표시된다.
     * @param message 발행할 메시지
     */
    private void publishNow(SseMessage message) {
        try {
            stringRedisTemplate.convertAndSend(SSE_CHANNEL, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            log.error("SSE 메시지 직렬화 실패 usrId={} event={}", message.usrId(), message.eventName(), e);
        } catch (RuntimeException e) {
            log.warn("Redis 발행 실패, 현재 인스턴스에만 전송 usrId={} event={} cause={}",
                    message.usrId(), message.eventName(), e.getMessage());
            sseEmitterRegistry.send(message.usrId(), message.eventName(), message.data());
        }
    }
}
