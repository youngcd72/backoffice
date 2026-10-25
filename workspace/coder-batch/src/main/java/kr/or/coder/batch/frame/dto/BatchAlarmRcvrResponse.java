package kr.or.coder.batch.frame.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 배치 알람수신자 1건 응답 (TB_DSPT_ALRM_RCVR_R + TB_USR_M)
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
public class BatchAlarmRcvrResponse {

    /** 사용자ID */
    private String usrId;
    /** 사용자명 */
    private String usrNm;
    /** 사용자 사용여부 (N 이면 알람을 받지 않음) */
    private String useYn;
}
