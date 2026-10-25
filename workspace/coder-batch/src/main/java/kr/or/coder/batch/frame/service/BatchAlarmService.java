package kr.or.coder.batch.frame.service;

/**
 * 배치 실패 알람 Service (TB_DSPT_ALRM_RCVR_R 에 등록된 사용자에게 TB_ALRM_L 알람 등록 + 실시간 전송)
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
public interface BatchAlarmService {

    /**
     * 배치 실행 실패를 배치직무에 등록된 알람 수신자에게 알린다.
     * 알람 처리 오류가 배치 실행 결과에 영향을 주지 않도록 예외를 던지지 않고 로그만 남긴다.
     * @param jobId 직무ID
     * @param errMsgCont 오류메시지 (없으면 null)
     */
    void saveFailureAlarm(String jobId, String errMsgCont);
}
