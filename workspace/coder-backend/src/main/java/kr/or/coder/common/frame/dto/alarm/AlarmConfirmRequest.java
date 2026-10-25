package kr.or.coder.common.frame.dto.alarm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 알람 확인(읽음) 처리 요청
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
public class AlarmConfirmRequest {

    /** 확인여부 (읽음 처리만 지원하므로 Y 만 허용) */
    @NotBlank
    @Pattern(regexp = "Y")
    private String cfmYn;
}
