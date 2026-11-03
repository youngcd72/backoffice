package kr.or.coder.biz.system.link.controller;

import kr.or.coder.biz.system.link.dto.LinkKeyHistResponse;
import kr.or.coder.biz.system.link.dto.LinkKeyIssueRequest;
import kr.or.coder.biz.system.link.dto.LinkKeyIssueResponse;
import kr.or.coder.biz.system.link.dto.LinkKeyResponse;
import kr.or.coder.biz.system.link.dto.LinkKeySaveRequest;
import kr.or.coder.biz.system.link.dto.LinkOrgInterfaceResponse;
import kr.or.coder.biz.system.link.dto.LinkOrgInterfaceSaveRequest;
import kr.or.coder.biz.system.link.dto.LinkOrgRequest;
import kr.or.coder.biz.system.link.dto.LinkOrgResponse;
import kr.or.coder.biz.system.link.dto.LinkOrgSaveRequest;
import kr.or.coder.biz.system.link.service.LinkKeyService;
import kr.or.coder.biz.system.link.service.LinkOrgService;
import kr.or.coder.common.auth.PrgmAuth;
import kr.or.coder.core.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SYLK010 API관리 연계기관·클라이언트 키·기관별 허용 인터페이스 Controller
 * @author ywkim
 * @since 2026.11.03
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.11.03  ywkim        최초 생성
 * </pre>
 */
@RestController
@PrgmAuth("SYLK010")
@RequestMapping("/api/v1/link-orgs")
@RequiredArgsConstructor
@Tag(name = "SYLK010 Link Organization", description = "API관리 - 연계기관·클라이언트 키·허용 인터페이스")
public class LinkOrgController {

    private final LinkOrgService linkOrgService;
    private final LinkKeyService linkKeyService;

    /**
     * 조회 조건에 맞는 연계기관 목록을 조회 (사용 가능 키 수 포함)
     * @param request 조회 조건 (keyword: 기관ID·기관명 부분 일치, chnlClsCd, useYn)
     * @return 연계기관 목록
     */
    @Operation(summary = "연계기관 목록 조회")
    @GetMapping
    public ApiResponse<List<LinkOrgResponse>> getLinkOrgList(@ModelAttribute LinkOrgRequest request) {
        return ApiResponse.success(linkOrgService.getLinkOrgList(request));
    }

    /**
     * 연계기관 1건을 조회
     * @param lnkOrgId 연계기관ID
     * @return 연계기관
     */
    @Operation(summary = "연계기관 조회")
    @GetMapping("/{lnkOrgId}")
    public ApiResponse<LinkOrgResponse> getLinkOrg(@PathVariable String lnkOrgId) {
        return ApiResponse.success(linkOrgService.getLinkOrg(lnkOrgId));
    }

    /**
     * 연계기관을 등록 또는 수정
     * @param request 연계기관 (isNew 로 등록/수정 구분)
     * @param authentication 로그인 사용자 인증 정보 (등록/수정자 ID 추출용)
     * @return 처리 결과
     */
    @Operation(summary = "연계기관 저장", description = "isNew=true 면 등록, false 면 수정. 저장 후 coder-api 캐시 삭제를 알린다")
    @PostMapping
    public ApiResponse<Void> saveLinkOrg(@Valid @RequestBody LinkOrgSaveRequest request, Authentication authentication) {
        linkOrgService.saveLinkOrg(request, authentication.getName());
        return ApiResponse.success(null);
    }

    /**
     * 연계기관의 클라이언트 키 목록을 조회 (암호화 비밀키 제외)
     * @param lnkOrgId 연계기관ID
     * @return 클라이언트 키 목록
     */
    @Operation(summary = "클라이언트 키 목록 조회")
    @GetMapping("/{lnkOrgId}/keys")
    public ApiResponse<List<LinkKeyResponse>> getLinkKeyList(@PathVariable String lnkOrgId) {
        return ApiResponse.success(linkKeyService.getLinkKeyList(lnkOrgId));
    }

    /**
     * 클라이언트 키를 발급 (비밀키 원문은 이 응답에만 담는다)
     * @param lnkOrgId 연계기관ID
     * @param request 유효기간
     * @param authentication 로그인 사용자 인증 정보 (발급자 ID 추출용)
     * @return 키ID(client_id), 비밀키 원문(client_secret), 만료일시
     */
    @Operation(summary = "클라이언트 키 발급", description = "client_secret 원문은 이 응답에만 있고 다시 조회할 수 없다")
    @PostMapping("/{lnkOrgId}/keys")
    public ApiResponse<LinkKeyIssueResponse> saveLinkKey(@PathVariable String lnkOrgId,
                                                         @Valid @RequestBody LinkKeyIssueRequest request,
                                                         Authentication authentication) {
        return ApiResponse.success(linkKeyService.saveLinkKey(lnkOrgId, request, authentication.getName()));
    }

    /**
     * 클라이언트 키의 상태·만료일·허용IP·호출 한도·비고를 수정
     * @param lnkOrgId 연계기관ID
     * @param keyId 키ID
     * @param request 수정 내용
     * @param authentication 로그인 사용자 인증 정보 (수정자 ID 추출용)
     * @return 처리 결과
     */
    @Operation(summary = "클라이언트 키 수정", description = "폐기된 키는 수정할 수 없다. 바뀐 항목은 키 이력에 남긴다")
    @PatchMapping("/{lnkOrgId}/keys/{keyId}")
    public ApiResponse<Void> saveLinkKeyDtl(@PathVariable String lnkOrgId, @PathVariable String keyId,
                                            @Valid @RequestBody LinkKeySaveRequest request,
                                            Authentication authentication) {
        linkKeyService.saveLinkKeyDtl(lnkOrgId, keyId, request, authentication.getName());
        return ApiResponse.success(null);
    }

    /**
     * 클라이언트 키 이력을 최근 순으로 조회
     * @param lnkOrgId 연계기관ID
     * @param keyId 키ID
     * @return 키 이력 목록
     */
    @Operation(summary = "클라이언트 키 이력 조회")
    @GetMapping("/{lnkOrgId}/keys/{keyId}/histories")
    public ApiResponse<List<LinkKeyHistResponse>> getLinkKeyHistList(@PathVariable String lnkOrgId,
                                                                     @PathVariable String keyId) {
        return ApiResponse.success(linkKeyService.getLinkKeyHistList(lnkOrgId, keyId));
    }

    /**
     * 전체 인터페이스와 연계기관의 허용 여부를 조회
     * @param lnkOrgId 연계기관ID
     * @return 전체 인터페이스 + 허용 여부
     */
    @Operation(summary = "기관별 허용 인터페이스 조회")
    @GetMapping("/{lnkOrgId}/interfaces")
    public ApiResponse<List<LinkOrgInterfaceResponse>> getLinkOrgInterfaceList(@PathVariable String lnkOrgId) {
        return ApiResponse.success(linkOrgService.getLinkOrgInterfaceList(lnkOrgId));
    }

    /**
     * 연계기관의 허용 인터페이스를 목록으로 전체 교체
     * @param lnkOrgId 연계기관ID
     * @param request 허용할 인터페이스ID 목록
     * @param authentication 로그인 사용자 인증 정보 (수정자 ID 추출용)
     * @return 처리 결과
     */
    @Operation(summary = "기관별 허용 인터페이스 저장", description = "목록에 없는 기존 허용은 사용 안 함(N)으로 바꾼다. 이미 발급된 토큰에는 반영되지 않는다")
    @PostMapping("/{lnkOrgId}/interfaces")
    public ApiResponse<Void> saveLinkOrgInterfaceList(@PathVariable String lnkOrgId,
                                                      @Valid @RequestBody LinkOrgInterfaceSaveRequest request,
                                                      Authentication authentication) {
        linkOrgService.saveLinkOrgInterfaceList(lnkOrgId, request, authentication.getName());
        return ApiResponse.success(null);
    }
}
