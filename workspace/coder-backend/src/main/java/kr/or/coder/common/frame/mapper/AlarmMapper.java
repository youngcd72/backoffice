package kr.or.coder.common.frame.mapper;

import kr.or.coder.common.frame.dto.alarm.AlarmRequest;
import kr.or.coder.common.frame.dto.alarm.AlarmResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알람내역(TB_ALRM_L) MyBatis Mapper
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
public interface AlarmMapper {

    /**
     * 수신자의 알람 목록을 최근 발송 순으로 조회
     * @param rcvrId 수신자ID
     * @param maxRows 최대 조회 건수
     * @return 알람 목록
     */
    List<AlarmResponse> selectAlarmList(@Param("rcvrId") String rcvrId, @Param("maxRows") int maxRows);

    /**
     * 수신자의 확인하지 않은 알람 건수를 조회
     * @param rcvrId 수신자ID
     * @return 미확인 알람 건수
     */
    int selectUnconfirmedAlarmCount(@Param("rcvrId") String rcvrId);

    /**
     * 사용자 테이블에 있는 사용자ID만 조회 (알람 수신자 검증)
     * @param usrIdList 사용자ID 목록
     * @return 존재하는 사용자ID 목록
     */
    List<String> selectExistingUsrIdList(@Param("usrIdList") List<String> usrIdList);

    /**
     * 알람일련번호를 채번
     * @return 다음 알람일련번호
     */
    long selectNextAlarmSn();

    /**
     * 알람을 등록
     * @param alrmSn 알람일련번호
     * @param rcvrId 수신자ID
     * @param item 알람 내용 (유형, 제목, 내용, 프로그램ID)
     * @param usrId 발송자ID
     * @param regDt 발송일시
     * @return 등록 건수
     */
    int insertAlarm(@Param("alrmSn") long alrmSn,
                    @Param("rcvrId") String rcvrId,
                    @Param("item") AlarmRequest item,
                    @Param("usrId") String usrId,
                    @Param("regDt") LocalDateTime regDt);

    /**
     * 수신자의 미확인 알람을 확인 처리 (알람일련번호가 없으면 전체)
     * @param rcvrId 수신자ID
     * @param alrmSn 알람일련번호 (null 이면 수신자의 미확인 알람 전체)
     * @return 수정 건수
     */
    int updateAlarmConfirm(@Param("rcvrId") String rcvrId, @Param("alrmSn") Long alrmSn);
}
