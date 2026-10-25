package kr.or.coder.batch.frame.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 배치 실행 상태 변경(중지/폐기) 요청 DTO
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
public class BatchExecutionStatusRequest {

    /** STOPPED: 실행 중지, ABANDONED: 폐기 (비정상 종료 건 정리) */
    @NotBlank
    @Pattern(regexp = "^(STOPPED|ABANDONED)$", message = "execStatCd는 STOPPED 또는 ABANDONED입니다.")
    private String execStatCd;
}
