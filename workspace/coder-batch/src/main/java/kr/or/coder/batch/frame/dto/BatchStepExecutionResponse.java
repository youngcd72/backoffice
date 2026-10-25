package kr.or.coder.batch.frame.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 배치 Step 실행 처리 현황 응답 DTO
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
public class BatchStepExecutionResponse {

    private Long stepExecutionId;
    private String stepName;
    private String status;
    private String exitCode;
    private String exitDescription;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long readCount;
    private long writeCount;
    private long commitCount;
    private long rollbackCount;
    private long readSkipCount;
    private long processSkipCount;
    private long writeSkipCount;
    private long filterCount;
}
