package kr.or.coder.batch.common.quartz;

import kr.or.coder.batch.frame.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 기동 시 TB_DSPT_JOB_M(배치직무기본)과 Quartz JobDetail/Trigger를 동기화한다.
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
@Component
@RequiredArgsConstructor
public class ScheduleSyncRunner {

    private final ScheduleService scheduleService;

    /**
     * 애플리케이션 기동 완료 시 배치 스케줄을 동기화한다. 실패해도 기동은 유지한다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            scheduleService.syncSchedules();
        } catch (Exception e) {
            // 동기화 실패 시에도 기존 QRTZ_* 스케줄로 계속 동작하도록 기동은 유지한다.
            log.error("[ScheduleSyncRunner] failed to synchronize batch schedules.", e);
        }
    }
}
