package kr.or.coder.batch.frame.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 배치 실행 이력 검색 조건 요청 DTO (기간, 업무모듈, 직무, 상태, 페이징)
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
public class BatchExecutionSearchRequest {

    /** 실행시작 기간 시작일자 (YYYYMMDD) */
    @Pattern(regexp = "^\\d{8}$", message = "stDt는 YYYYMMDD 형식입니다.")
    private String stDt;

    /** 실행시작 기간 종료일자 (YYYYMMDD) */
    @Pattern(regexp = "^\\d{8}$", message = "endDt는 YYYYMMDD 형식입니다.")
    private String endDt;

    private String wrkMdulCd;
    private String jobId;
    private String execStatCd;
    private String execClsCd;

    /** 1부터 시작 */
    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(500)
    private int size = 20;

    /**
     * 페이징 조회용 시작 위치(offset)를 계산한다.
     * @return (page - 1) * size
     */
    public int getOffset() {
        return (page - 1) * size;
    }
}
