package kr.or.coder.common.frame.dto.alarm;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 알람 1건 응답 (TB_ALRM_L, 알림 센터 목록과 SSE alarm 이벤트 데이터)
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
@Getter
@Setter
@NoArgsConstructor
public class AlarmResponse {

    /** 알람일련번호 */
    private Long alrmSn;

    /** 알람유형코드 (SY/ALRM_TYPE_CD) */
    private String alrmTypeCd;

    /** 알람제목명 */
    private String alrmTitlNm;

    /** 알람내용 */
    private String alrmCont;

    /** 프로그램ID (클릭 시 열 화면) */
    private String prgmId;

    /** 확인여부 (Y/N) */
    private String cfmYn;

    /** 발송자ID */
    private String firsRegrId;

    /** 발송일시 */
    private LocalDateTime firsRegDt;
}
