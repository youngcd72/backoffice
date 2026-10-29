package kr.or.coder.api.biz.system.sample.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 샘플 업무 API 응답 - 공통코드 상세 한 건
 * @author ywkim
 * @since 2026.10.27
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.27  ywkim        최초 생성
 * </pre>
 */
@Getter
@Setter
public class SampleCodeResponse {

    private String commCtgDtlCd;
    private String commCtgDtlNm;
    private Integer alinSeq;
}
