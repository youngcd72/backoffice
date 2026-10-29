package kr.or.coder.api.biz.system.sample.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.or.coder.api.biz.system.sample.dto.SampleResponse;
import kr.or.coder.api.biz.system.sample.service.SampleService;
import kr.or.coder.api.common.annotation.ApiInterface;
import kr.or.coder.core.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 샘플 업무 API Controller (조회형). 업무 API를 추가할 때 이 구조를 따른다.
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
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/samples")
@Tag(name = "Samples", description = "샘플 업무 API (공통코드 조회)")
public class SampleController {

    private final SampleService sampleService;

    /**
     * 공통코드 그룹과 사용 중인 상세코드 목록을 조회한다.
     * @param id 공통분류코드
     * @param wrkMdulCd 업무모듈코드 (기본 SY)
     * @return 공통코드 그룹과 상세코드 목록
     */
    @Operation(summary = "샘플 조회 (IFSY0001)", description = "공통코드 그룹과 사용 중인 상세코드 목록을 조회한다. 없으면 404.")
    @ApiInterface("IFSY0001")
    @GetMapping("/{id}")
    public ApiResponse<SampleResponse> getSample(@PathVariable String id,
                                                 @RequestParam(defaultValue = "SY") String wrkMdulCd) {
        return ApiResponse.success(sampleService.getSample(wrkMdulCd, id));
    }
}
