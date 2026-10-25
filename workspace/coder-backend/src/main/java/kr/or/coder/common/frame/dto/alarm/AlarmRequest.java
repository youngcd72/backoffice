package kr.or.coder.common.frame.dto.alarm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 알람 발송 요청 (수신자별로 TB_ALRM_L 1건씩 등록)
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
public class AlarmRequest {

    /** 수신자ID 목록 */
    @NotEmpty
    @Size(max = 1000)
    private List<@NotBlank @Size(max = 10) String> rcvrIdList;

    /** 알람유형코드 (SY/ALRM_TYPE_CD) */
    @NotBlank
    @Size(max = 20)
    private String alrmTypeCd;

    /** 알람제목명 */
    @NotBlank
    @Size(max = 200)
    private String alrmTitlNm;

    /** 알람내용 */
    @Size(max = 4000)
    private String alrmCont;

    /** 프로그램ID (클릭 시 열 화면, 없으면 이동 없음) */
    @Size(max = 100)
    private String prgmId;
}
