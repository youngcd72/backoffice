package kr.or.coder.api.common.calllog;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import kr.or.coder.api.common.calllog.mapper.CallLogMapper;
import kr.or.coder.api.common.config.ApiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 호출 이력을 메모리 큐에 넣고, 별도 스레드가 일정 간격으로 모아 일괄 INSERT 한다 (요청 처리 스레드는 DB를 기다리지 않는다).
 * 큐가 가득 차면 이력을 버리고 버린 건수를 WARN 로그로 남긴다. 종료 시 남은 이력을 모두 저장한다.
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
@Slf4j
@Component
public class CallLogRecorder {

    private final CallLogMapper callLogMapper;
    private final ApiProperties.CallLog properties;
    private final BlockingQueue<CallLog> queue;
    private final AtomicLong droppedCount = new AtomicLong();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "call-log-flush");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * 매퍼와 호출 이력 설정으로 생성
     * @param callLogMapper 호출 이력 매퍼
     * @param apiProperties 대외 연계 API 설정
     */
    public CallLogRecorder(CallLogMapper callLogMapper, ApiProperties apiProperties) {
        this.callLogMapper = callLogMapper;
        this.properties = apiProperties.getCallLog();
        this.queue = new ArrayBlockingQueue<>(properties.getQueueCapacity());
    }

    /**
     * 일정 간격 저장 스레드를 시작
     */
    @PostConstruct
    public void start() {
        long interval = properties.getFlushIntervalMs();
        scheduler.scheduleWithFixedDelay(this::flush, interval, interval, TimeUnit.MILLISECONDS);
    }

    /**
     * 호출 이력을 큐에 넣는다 (큐가 가득 차면 버린다)
     * @param callLog 호출 이력
     */
    public void record(CallLog callLog) {
        if (!queue.offer(callLog)) {
            droppedCount.incrementAndGet();
        }
    }

    /**
     * 큐에 쌓인 이력을 batch-size 단위로 저장 (저장 실패는 ERROR 로그만 남기고 다음 묶음을 계속 저장)
     */
    void flush() {
        long dropped = droppedCount.getAndSet(0);
        if (dropped > 0) {
            log.warn("call log queue is full. {} call logs were dropped", dropped);
        }
        while (!queue.isEmpty()) {
            List<CallLog> batch = new ArrayList<>(properties.getBatchSize());
            queue.drainTo(batch, properties.getBatchSize());
            if (batch.isEmpty()) {
                return;
            }
            try {
                callLogMapper.insertCallLogList(batch);
            } catch (Exception e) {
                log.error("failed to save {} call logs", batch.size(), e);
            }
        }
    }

    /**
     * 저장 스레드를 멈추고 남은 이력을 저장
     * @throws InterruptedException 저장 스레드 종료를 기다리는 중 인터럽트된 경우
     */
    @PreDestroy
    public void stop() throws InterruptedException {
        scheduler.shutdown();
        scheduler.awaitTermination(10, TimeUnit.SECONDS);
        flush();
    }
}
