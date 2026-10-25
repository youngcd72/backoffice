package kr.or.coder.batch.frame.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 배치직무 검색 조건 요청 DTO
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
public class BatchJobSearchRequest {

    private String wrkMdulCd;
    private String useYn;
    /** 직무ID / 직무명 부분 일치 */
    private String keyword;
    /** 최종 실행시작일자 (YYYYMMDD) */
    private String lastExecDt;
    /** 최종 실행상태코드 */
    private String lastExecStatCd;
}
