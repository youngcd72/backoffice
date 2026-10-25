package kr.or.coder.common.frame.service;

import kr.or.coder.common.frame.dto.alarm.AlarmRequest;
import kr.or.coder.common.frame.dto.alarm.AlarmResponse;
import kr.or.coder.core.common.exception.BusinessException;

import java.util.List;

/**
 * 알림 센터 알람 조회·발송·확인 Service
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
public interface AlarmService {

    /**
     * 사용자의 최근 알람 목록을 조회
     * @param usrId 사용자ID
     * @return 알람 목록 (최근 발송 순)
     */
    List<AlarmResponse> getAlarmList(String usrId);

    /**
     * 사용자의 확인하지 않은 알람 건수를 조회
     * @param usrId 사용자ID
     * @return 미확인 알람 건수
     */
    int getUnconfirmedAlarmCount(String usrId);

    /**
     * 수신자별로 알람을 등록하고, 커밋 후 접속 중인 수신자에게 SSE 로 전송
     * @param request 알람 발송 요청 (수신자 목록, 유형, 제목, 내용, 프로그램ID)
     * @param usrId 발송자ID
     * @return 등록 건수
     * @throws BusinessException 사용자 테이블에 없는 수신자가 있는 경우
     */
    int saveAlarm(AlarmRequest request, String usrId);

    /**
     * 사용자의 알람을 확인 처리하고, 다른 탭에도 미확인 건수를 전송
     * @param usrId 사용자ID
     * @param alrmSn 알람일련번호 (null 이면 미확인 알람 전체)
     * @return 수정 건수
     */
    int saveAlarmConfirm(String usrId, Long alrmSn);
}
