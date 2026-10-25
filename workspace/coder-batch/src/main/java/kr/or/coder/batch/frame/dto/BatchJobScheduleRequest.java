package kr.or.coder.batch.frame.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 배치직무 스케줄 상태 변경(일시정지/재개) 요청 DTO
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobScheduleRequest {

    /** PAUSED: 일시정지, NORMAL: 재개 */
    @NotBlank
    @Pattern(regexp = "^(PAUSED|NORMAL)$", message = "schedStatus는 PAUSED 또는 NORMAL입니다.")
    private String schedStatus;
}
