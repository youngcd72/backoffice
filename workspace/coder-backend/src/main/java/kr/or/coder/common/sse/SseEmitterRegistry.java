package kr.or.coder.common.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 이 인스턴스에 접속 중인 SSE 연결(SseEmitter)을 사용자별로 보관하고 이벤트를 전송
 * 사용자 1명이 여러 탭으로 접속할 수 있어 사용자당 emitter 목록으로 관리한다.
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
public class SseEmitterRegistry {

    private final Map<String, List<SseEmitter>> emitterMap = new ConcurrentHashMap<>();

    /** 연결 유지 시간(ms). 지나면 서버가 연결을 닫고 화면이 다시 연결한다 */
    @Value("${coder.sse.timeout-ms:1800000}")
    private long timeoutMs;

    /** 사용자당 최대 연결 수. 넘으면 가장 오래된 연결을 닫는다 */
    @Value("${coder.sse.max-emitters-per-user:10}")
    private int maxEmittersPerUser;

    /**
     * 사용자의 SSE 연결을 생성해 등록 (완료·타임아웃·오류 시 자동 제거)
     * @param usrId 사용자ID
     * @return 등록한 SseEmitter
     */
    public SseEmitter subscribe(String usrId) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        List<SseEmitter> overflowList = new ArrayList<>();

        emitterMap.compute(usrId, (key, list) -> {
            List<SseEmitter> emitters = list == null ? new CopyOnWriteArrayList<>() : list;
            emitters.add(emitter);
            while (emitters.size() > maxEmittersPerUser) {
                overflowList.add(emitters.remove(0));
            }
            return emitters;
        });
        overflowList.forEach(SseEmitter::complete);

        emitter.onCompletion(() -> remove(usrId, emitter));
        // 타임아웃 시 직접 완료해야 AsyncRequestTimeoutException 이 발생하지 않는다
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> remove(usrId, emitter));
        return emitter;
    }

    /**
     * 사용자의 접속 중인 모든 연결에 이벤트를 전송 (이 인스턴스에 연결이 없으면 아무 것도 하지 않음)
     * @param usrId 사용자ID
     * @param eventName 이벤트명
     * @param data 이벤트 데이터 (JSON 으로 직렬화)
     */
    public void send(String usrId, String eventName, Object data) {
        List<SseEmitter> emitters = emitterMap.get(usrId);
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            send(usrId, emitter, SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
        }
    }

    /**
     * 지정한 연결 1개에 이벤트를 전송 (연결 직후 초기 데이터 전송용)
     * @param usrId 사용자ID
     * @param emitter 대상 연결
     * @param eventName 이벤트명
     * @param data 이벤트 데이터 (JSON 으로 직렬화)
     */
    public void send(String usrId, SseEmitter emitter, String eventName, Object data) {
        send(usrId, emitter, SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
    }

    /**
     * 모든 연결에 주석(heartbeat)을 보내 프록시·로드밸런서가 유휴 연결을 끊지 않게 하고, 끊긴 연결을 정리
     */
    @Scheduled(fixedDelayString = "${coder.sse.heartbeat-interval-ms:25000}")
    public void sendHeartbeat() {
        emitterMap.forEach((usrId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                send(usrId, emitter, SseEmitter.event().comment("ping"));
            }
        });
    }

    /**
     * 이 인스턴스에 접속 중인 연결 수를 조회
     * @return 전체 연결 수
     */
    public int getEmitterCount() {
        return emitterMap.values().stream().mapToInt(List::size).sum();
    }

    /**
     * 연결에 이벤트를 전송하고, 실패하면(브라우저가 닫힌 경우 등) 목록에서 제거
     * 끊긴 연결은 예상된 상황이므로 ERROR 가 아닌 DEBUG 로 남긴다 (컨테이너가 요청을 마무리한다).
     * @param usrId 사용자ID
     * @param emitter 대상 연결
     * @param event 전송할 이벤트
     */
    private void send(String usrId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE 전송 실패, 연결 제거 usrId={} cause={}", usrId, e.getMessage());
            remove(usrId, emitter);
        }
    }

    /**
     * 사용자 연결 목록에서 emitter 를 제거 (목록이 비면 사용자 항목도 제거)
     * @param usrId 사용자ID
     * @param emitter 제거할 연결
     */
    private void remove(String usrId, SseEmitter emitter) {
        emitterMap.computeIfPresent(usrId, (key, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
