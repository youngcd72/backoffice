package kr.or.coder.biz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.or.coder.common.frame.dto.auth.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 인증(로그인/JWT) 통합 테스트
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
@SpringBootTest
@AutoConfigureMockMvc
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 인증 없이 /api/ping 에 접근할 수 있는지 검증
     * @throws Exception MockMvc 수행 중 예외
     */
    @Test
    @DisplayName("Public endpoint /api/ping should be accessible without authentication")
    void testPublicPingEndpoint() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    /**
     * 토큰 없이 보호된 엔드포인트 호출 시 401 이 반환되는지 검증
     * @throws Exception MockMvc 수행 중 예외
     */
    @Test
    @DisplayName("Protected endpoint /api/v1/favorites should return 401 Unauthorized when no token is provided")
    void testProtectedEndpointUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/favorites"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    /**
     * 로그인으로 발급된 JWT 로 보호된 엔드포인트에 접근할 수 있는지 검증
     * @throws Exception MockMvc 수행 중 예외
     */
    @Test
    @DisplayName("Initial Login should issue a valid signed JWT and grant access to protected endpoints")
    void testInitialLoginAndAuthenticatedAccess() throws Exception {
        // 1. Initial Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("coder1234");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = responseJson.path("data").path("accessToken").asText();

        assertThat(accessToken).isNotBlank();
        // JWT structure consists of 3 parts separated by dots: header.payload.signature
        assertThat(accessToken.split("\\.")).hasSize(3);

        // 2. Access protected endpoint using the newly issued JWT token
        mockMvc.perform(get("/api/v1/favorites")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * 잘못된 비밀번호로 로그인하면 401 이 반환되는지 검증
     * @throws Exception MockMvc 수행 중 예외
     */
    @Test
    @DisplayName("Login with invalid password should fail with 401 Unauthorized")
    void testLoginWithInvalidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
}
