package kr.or.coder.common.frame.service;

import kr.or.coder.common.frame.dto.alarm.AlarmRequest;
import kr.or.coder.common.frame.dto.alarm.AlarmResponse;
import kr.or.coder.common.frame.mapper.AlarmMapper;
import kr.or.coder.common.sse.SsePublisher;
import kr.or.coder.core.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 알림 센터 알람 조회·발송·확인 Service 구현
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
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlarmServiceImpl implements AlarmService {

    /** SSE 이벤트명: 새 알람 1건 */
    public static final String EVENT_ALARM = "alarm";

    /** SSE 이벤트명: 미확인 알람 건수 (연결 직후, 확인 처리 후) */
    public static final String EVENT_COUNT = "count";

    private final AlarmMapper alarmMapper;
    private final SsePublisher ssePublisher;

    /** 알림 센터 목록 최대 조회 건수 (coder.alarm.max-rows) */
    @Value("${coder.alarm.max-rows:50}")
    private int maxAlarmRows;

    /**
     * 사용자의 최근 알람 목록을 조회
     * @param usrId 사용자ID
     * @return 알람 목록 (최근 발송 순, 최대 50건)
     */
    @Override
    public List<AlarmResponse> getAlarmList(String usrId) {
        return alarmMapper.selectAlarmList(usrId, maxAlarmRows);
    }

    /**
     * 사용자의 확인하지 않은 알람 건수를 조회
     * @param usrId 사용자ID
     * @return 미확인 알람 건수
     */
    @Override
    public int getUnconfirmedAlarmCount(String usrId) {
        return alarmMapper.selectUnconfirmedAlarmCount(usrId);
    }

    /**
     * 수신자별로 알람을 등록하고, 커밋 후 접속 중인 수신자에게 SSE 로 전송
     * @param request 알람 발송 요청 (수신자 목록, 유형, 제목, 내용, 프로그램ID)
     * @param usrId 발송자ID
     * @return 등록 건수
     * @throws BusinessException 사용자 테이블에 없는 수신자가 있는 경우
     */
    @Override
    @Transactional
    public int saveAlarm(AlarmRequest request, String usrId) {
        List<String> rcvrIdList = List.copyOf(new LinkedHashSet<>(request.getRcvrIdList()));
        Set<String> existingIdSet = new HashSet<>(alarmMapper.selectExistingUsrIdList(rcvrIdList));
        for (String rcvrId : rcvrIdList) {
            if (!existingIdSet.contains(rcvrId)) {
                throw new BusinessException("cm.alarm.receiver.not.found", rcvrId);
            }
        }

        LocalDateTime regDt = LocalDateTime.now();
        for (String rcvrId : rcvrIdList) {
            long alrmSn = alarmMapper.selectNextAlarmSn();
            alarmMapper.insertAlarm(alrmSn, rcvrId, request, usrId, regDt);
            ssePublisher.publish(rcvrId, EVENT_ALARM, toAlarmResponse(alrmSn, request, usrId, regDt));
        }
        return rcvrIdList.size();
    }

    /**
     * 사용자의 알람을 확인 처리하고, 다른 탭에도 미확인 건수를 전송
     * @param usrId 사용자ID
     * @param alrmSn 알람일련번호 (null 이면 미확인 알람 전체)
     * @return 수정 건수 (이미 확인했거나 본인 알람이 아니면 0)
     */
    @Override
    @Transactional
    public int saveAlarmConfirm(String usrId, Long alrmSn) {
        int updatedCnt = alarmMapper.updateAlarmConfirm(usrId, alrmSn);
        if (updatedCnt > 0) {
            ssePublisher.publish(usrId, EVENT_COUNT, alarmMapper.selectUnconfirmedAlarmCount(usrId));
        }
        return updatedCnt;
    }

    /**
     * 등록한 알람을 SSE alarm 이벤트 데이터로 변환
     * @param alrmSn 알람일련번호
     * @param request 알람 발송 요청
     * @param usrId 발송자ID
     * @param regDt 발송일시
     * @return 알람 응답
     */
    private AlarmResponse toAlarmResponse(long alrmSn, AlarmRequest request, String usrId, LocalDateTime regDt) {
        AlarmResponse alarm = new AlarmResponse();
        alarm.setAlrmSn(alrmSn);
        alarm.setAlrmTypeCd(request.getAlrmTypeCd());
        alarm.setAlrmTitlNm(request.getAlrmTitlNm());
        alarm.setAlrmCont(request.getAlrmCont());
        alarm.setPrgmId(request.getPrgmId());
        alarm.setCfmYn("N");
        alarm.setFirsRegrId(usrId);
        alarm.setFirsRegDt(regDt);
        return alarm;
    }
}
