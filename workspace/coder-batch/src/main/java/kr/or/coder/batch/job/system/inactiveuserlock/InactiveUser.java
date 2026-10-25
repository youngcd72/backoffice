package kr.or.coder.batch.job.system.inactiveuserlock;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 장기 미접속 사용자 (TB_USR_M 일부 컬럼)
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
@Getter
@Setter
@NoArgsConstructor
public class InactiveUser {

    private String usrId;
    private String usrNm;
    /** 최종접속일시 */
    private LocalDateTime finaAccDttm;
    /** 사용자잠금일시 (Processor에서 설정) */
    private LocalDateTime usrLockDttm;
    /** 최종등록자ID (Processor에서 설정) */
    private String finaRegrId;
}
