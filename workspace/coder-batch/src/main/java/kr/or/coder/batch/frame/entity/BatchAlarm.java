package kr.or.coder.batch.frame.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 배치 실패 알람 1건 (TB_ALRM_L 등록 대상)
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAlarm {

    /** 알람일련번호 */
    private Long alrmSn;
    /** 수신자ID */
    private String rcvrId;
    /** 알람유형코드 (SY/ALRM_TYPE_CD) */
    private String alrmTypeCd;
    /** 알람제목명 */
    private String alrmTitlNm;
    /** 알람내용 */
    private String alrmCont;
    /** 프로그램ID (클릭 시 열 화면) */
    private String prgmId;
    /** 발송자ID (최초/최종 등록자) */
    private String regrId;
    /** 발송일시 (최초/최종 등록일시) */
    private LocalDateTime regDt;
}
