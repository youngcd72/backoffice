package kr.or.coder.batch.frame.mapper;

import kr.or.coder.batch.frame.dto.BatchAlarmRcvrResponse;
import kr.or.coder.batch.frame.entity.BatchAlarm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 배치 실패 알람 Mapper (TB_DSPT_ALRM_RCVR_R 수신자 조회·저장, TB_ALRM_L 알람 등록)
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
@Mapper
public interface BatchAlarmMapper {

    /**
     * 배치직무에 등록된 알람 수신자 중 사용 중인 사용자ID 목록을 조회한다.
     * @param jobId 직무ID
     * @return 수신자ID 목록 (사용자ID 순)
     */
    List<String> selectBatchAlarmRcvrIdList(@Param("jobId") String jobId);

    /**
     * 알람일련번호를 채번한다.
     * @return 다음 알람일련번호
     */
    long selectNextAlarmSn();

    /**
     * 알람을 등록한다.
     * @param alarm 등록할 알람 (일련번호, 수신자, 유형, 제목, 내용, 프로그램ID, 발송자, 발송일시)
     * @return 등록 건수
     */
    int insertAlarm(BatchAlarm alarm);

    /**
     * 배치직무에 등록된 알람 수신자 목록을 사용자명과 함께 조회한다 (배치관리 화면 표시용, 미사용 사용자 포함).
     * @param jobId 직무ID
     * @return 알람 수신자 목록 (사용자ID 순)
     */
    List<BatchAlarmRcvrResponse> selectBatchAlarmRcvrList(@Param("jobId") String jobId);

    /**
     * 사용자 테이블에 있는 사용자ID만 조회한다 (알람 수신자 검증).
     * @param usrIdList 사용자ID 목록
     * @return 존재하는 사용자ID 목록
     */
    List<String> selectExistingUsrIdList(@Param("usrIdList") List<String> usrIdList);

    /**
     * 배치직무의 알람 수신자를 등록한다.
     * @param jobId 직무ID
     * @param usrId 수신자 사용자ID
     * @param regrId 등록자ID
     * @return 등록 건수
     */
    int insertBatchAlarmRcvr(@Param("jobId") String jobId, @Param("usrId") String usrId, @Param("regrId") String regrId);

    /**
     * 배치직무의 알람 수신자를 모두 삭제한다 (배치직무 삭제, 수신자 저장 시 전체 교체).
     * @param jobId 직무ID
     * @return 삭제 건수
     */
    int deleteBatchAlarmRcvrByJobId(@Param("jobId") String jobId);
}
