package kr.or.coder.common.frame.controller;

import kr.or.coder.common.frame.dto.favorite.FavoriteResponse;
import kr.or.coder.common.frame.service.FavoriteService;
import kr.or.coder.core.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 로그인 사용자의 즐겨찾기 메뉴 조회 Controller
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
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "CODER User Favorite Menu APIs")
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * 로그인 사용자(JWT)의 즐겨찾기 메뉴 목록을 조회
     * @param authentication 인증 정보 (사용자ID)
     * @return 즐겨찾기 메뉴 목록 (정렬순번 순)
     */
    @Operation(summary = "Get My Favorites", description = "Returns favorite menus of the signed-in user")
    @GetMapping("/favorites")
    public ApiResponse<List<FavoriteResponse>> getFavoriteList(Authentication authentication) {
        return ApiResponse.success(favoriteService.getFavoriteList(authentication.getName()));
    }
}
