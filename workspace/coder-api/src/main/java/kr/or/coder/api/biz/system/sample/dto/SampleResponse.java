package kr.or.coder.api.biz.system.sample.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 샘플 업무 API 응답 - 공통코드 그룹과 상세코드 목록
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
public class SampleResponse {

    private String wrkMdulCd;
    private String commCtgCd;
    private String commCtgNm;
    private List<SampleCodeResponse> codeList;
}
