package kr.or.coder.api.common.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.or.coder.core.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * coder-api 생존 확인 Controller
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
@Tag(name = "System", description = "CODER API Health Check")
public class SystemController {

    /**
     * coder-api 생존 여부를 확인하는 단순 헬스 체크
     * @return 상태, 서비스명, 현재 시각(밀리초)
     */
    @Operation(summary = "Ping CODER API")
    @SecurityRequirements
    @GetMapping("/api/ping")
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.success("CODER API is alive and responsive.", Map.of(
                "status", "UP",
                "service", "coder-api",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
