package kr.or.coder.common.frame.controller;

import kr.or.coder.core.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 시스템 상태 확인(Health Check) Controller
 * @author ywkim
 * @since 2026.09.30
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.09.30  ywkim        최초 생성
 * </pre>
 */
@RestController
@Tag(name = "System", description = "CODER System Health Check API")
public class SystemController {

    /**
     * 백엔드 생존 여부를 확인하는 단순 헬스 체크
     * @return 상태, 서비스명, 현재 시각(밀리초)
     */
    @Operation(summary = "Ping CODER Backend", description = "Simple health check endpoint used by Frontend console")
    @GetMapping("/api/ping")
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.success("CODER Backend is alive and responsive.", Map.of(
                "status", "UP",
                "service", "coder-backend",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
