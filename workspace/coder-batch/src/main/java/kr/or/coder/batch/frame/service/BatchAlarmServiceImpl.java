package kr.or.coder.batch.frame.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.or.coder.batch.common.config.BatchProperties;
import kr.or.coder.batch.common.support.BatchConstants;
import kr.or.coder.batch.common.support.BatchTextUtil;
import kr.or.coder.batch.frame.entity.BatchAlarm;
import kr.or.coder.batch.frame.entity.BatchJob;
import kr.or.coder.batch.frame.mapper.BatchAlarmMapper;
import kr.or.coder.batch.frame.mapper.BatchJobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 배치 실패 알람 Service 구현
 * 수신자별로 TB_ALRM_L 에 등록(건별 커밋)한 뒤 Redis 채널 coder:sse 로 발행하면 coder-backend 가 접속 중인 사용자에게 SSE 로 전달한다.
 * 알람은 DB 에 남으므로 Redis 발행이 실패해도 다음 알림 센터 조회 때 표시된다.
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
@Service
@RequiredArgsConstructor
public class BatchAlarmServiceImpl implements BatchAlarmService {

    private final BatchAlarmMapper batchAlarmMapper;
    private final BatchJobMapper batchJobMapper;
    private final BatchProperties batchProperties;
    private final MessageSource messageSource;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /** 알람 문구 언어 (Quartz 스레드에는 요청 Locale 이 없어 설정된 기본 언어를 쓴다) */
    @Value("${coder.message.default-locale:ko}")
    private String defaultLocale;

    /**
     * 배치 실행 실패를 배치직무에 등록된 알람 수신자에게 알린다.
     * 알람 처리 오류가 배치 실행 결과에 영향을 주지 않도록 예외를 던지지 않고 로그만 남긴다.
     * @param jobId 직무ID
     * @param errMsgCont 오류메시지 (없으면 null)
     */
    @Override
    public void saveFailureAlarm(String jobId, String errMsgCont) {
        try {
            List<String> rcvrIdList = batchAlarmMapper.selectBatchAlarmRcvrIdList(jobId);
            if (rcvrIdList.isEmpty()) {
                log.debug("[{}] no alarm receivers registered. failure alarm skipped.", jobId);
                return;
            }

            BatchJob batchJob = batchJobMapper.selectBatchJob(jobId);
            String jobNm = batchJob != null ? batchJob.getJobNm() : jobId;
            Locale locale = Locale.forLanguageTag(defaultLocale);
            String alrmTitlNm = messageSource.getMessage("sy.batch.alarm.failed.title", new Object[]{jobId, jobNm}, locale);
            String alrmCont = StringUtils.hasText(errMsgCont)
                    ? errMsgCont
                    : messageSource.getMessage("sy.batch.alarm.failed.no.message", null, locale);
            String regrId = BatchTextUtil.limitUserId(batchProperties.getSystemUserId());
            LocalDateTime regDt = LocalDateTime.now();

            List<BatchAlarm> alarmList = new ArrayList<>();
            for (String rcvrId : rcvrIdList) {
                BatchAlarm alarm = BatchAlarm.builder()
                        .alrmSn(batchAlarmMapper.selectNextAlarmSn())
                        .rcvrId(rcvrId)
                        .alrmTypeCd(BatchConstants.ALRM_TYPE_DSPT)
                        .alrmTitlNm(alrmTitlNm)
                        .alrmCont(alrmCont)
                        .prgmId(BatchConstants.ALRM_PRGM_ID)
                        .regrId(regrId)
                        .regDt(regDt)
                        .build();
                batchAlarmMapper.insertAlarm(alarm);
                alarmList.add(alarm);
            }
            log.info("[{}] failure alarm saved. receivers={}", jobId, rcvrIdList);

            alarmList.forEach(alarm -> publishAlarm(jobId, alarm));
        } catch (Exception e) {
            log.error("[" + jobId + "] failed to save batch failure alarm.", e);
        }
    }

    /**
     * 알람을 coder-backend SSE 메시지 형식(SseMessage: usrId, eventName, data=AlarmResponse)으로 Redis 에 발행한다.
     * 발행 실패는 알람이 DB 에 남아 있으므로 WARN 으로만 남긴다.
     * @param jobId 직무ID (로그용)
     * @param alarm 발행할 알람
     */
    private void publishAlarm(String jobId, BatchAlarm alarm) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("alrmSn", alarm.getAlrmSn());
        data.put("alrmTypeCd", alarm.getAlrmTypeCd());
        data.put("alrmTitlNm", alarm.getAlrmTitlNm());
        data.put("alrmCont", alarm.getAlrmCont());
        data.put("prgmId", alarm.getPrgmId());
        data.put("cfmYn", BatchConstants.NO);
        data.put("firsRegrId", alarm.getRegrId());
        data.put("firsRegDt", alarm.getRegDt());

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("usrId", alarm.getRcvrId());
        message.put("eventName", BatchConstants.SSE_EVENT_ALARM);
        message.put("data", data);

        try {
            stringRedisTemplate.convertAndSend(BatchConstants.SSE_CHANNEL, objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            log.warn("[{}] failed to publish failure alarm to Redis. rcvrId={}, alrmSn={}, cause={}",
                    jobId, alarm.getRcvrId(), alarm.getAlrmSn(), e.getMessage());
        }
    }
}
