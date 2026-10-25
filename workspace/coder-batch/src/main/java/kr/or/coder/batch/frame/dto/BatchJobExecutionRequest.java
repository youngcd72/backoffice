package kr.or.coder.batch.frame.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * 배치직무 수동 실행 요청 DTO
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
public class BatchJobExecutionRequest {

    /** 수동 실행 JobParameters. 배치직무 기본 파라미터(JOB_PARM_CONT)보다 우선한다. 예) {"baseDt":"20261008"} */
    private Map<String, String> params;
}
