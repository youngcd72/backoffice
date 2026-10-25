package kr.or.coder.batch.frame.controller;

import kr.or.coder.core.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BatchJobController 통합 테스트.
 * PostgreSQL에 schema/batch/*.sql, schema/table/TB_DSPT_*.sql 반영 후 실행한다.
 * <pre>./gradlew integrationTest</pre>
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
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class BatchJobControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * 토큰 없이 배치 관리 API를 호출하면 401이 반환되는지 검증한다.
     * @throws Exception MockMvc 호출 중 오류가 발생한 경우
     */
    @Test
    @DisplayName("토큰 없이 배치 관리 API를 호출하면 401")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/batch-jobs"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 관리자 권한이 아니면 403이 반환되는지 검증한다.
     * @throws Exception MockMvc 호출 중 오류가 발생한 경우
     */
    @Test
    @DisplayName("관리자 권한이 아니면 403")
    void forbidden() throws Exception {
        String token = jwtTokenProvider.generateToken("viewer", "Viewer", "VIEWER", List.of(), null);
        mockMvc.perform(get("/api/v1/batch-jobs").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    /**
     * 관리자가 배치직무 목록을 조회할 수 있는지 검증한다.
     * @throws Exception MockMvc 호출 중 오류가 발생한 경우
     */
    @Test
    @DisplayName("관리자는 배치직무 목록을 조회한다")
    void getBatchJobList() throws Exception {
        String token = jwtTokenProvider.generateToken("admin", "Admin", "ADMIN", List.of("ALL"), null);
        mockMvc.perform(get("/api/v1/batch-jobs").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}
