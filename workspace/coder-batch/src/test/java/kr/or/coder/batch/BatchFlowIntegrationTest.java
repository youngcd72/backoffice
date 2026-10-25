package kr.or.coder.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.or.coder.batch.support.TestSchemaInitializer;
import kr.or.coder.core.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PostgreSQL 임시 스키마(TestSchemaInitializer)로 기동 → 스케줄 동기화 → 관리 API → Quartz 수동 실행 → Spring Batch → 실행내역 기록까지 검증한다.
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
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestSchemaInitializer.class)
class BatchFlowIntegrationTest {

    private static final long WAIT_TIMEOUT_MILLIS = 15_000;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;

    /**
     * 각 테스트 전에 관리자(ADMIN) JWT 토큰을 발급한다.
     */
    @BeforeEach
    void setUp() {
        adminToken = "Bearer " + jwtTokenProvider.generateToken("admin", "Admin", "ADMIN", List.of("ALL"), null);
    }

    /**
     * 기동 시 USE_YN='Y' 배치직무만 CronTrigger가 등록되는지 검증한다.
     * @throws Exception MockMvc 호출 중 오류가 발생한 경우
     */
    @Test
    @DisplayName("기동 시 USE_YN='Y' 배치직무만 CronTrigger가 등록된다")
    void scheduleSynchronizedOnStartup() throws Exception {
        mockMvc.perform(get("/api/v1/batch-jobs/BHFL002").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobBeanYn").value("Y"))
                .andExpect(jsonPath("$.data.execClassNm").value("kr.or.coder.batch.job.system.execlogpurge.ExecLogPurgeJobConfig"))
                .andExpect(jsonPath("$.data.schedStatus").value("NORMAL"))
                .andExpect(jsonPath("$.data.nextFireDttm").isNotEmpty());

        mockMvc.perform(get("/api/v1/batch-jobs/BHFL003").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.schedStatus").value("NONE"));
    }

    /**
     * 토큰이 없으면 401, 권한 없는 사용자는 403이 반환되는지 검증한다.
     * @throws Exception MockMvc 호출 중 오류가 발생한 경우
     */
    @Test
    @DisplayName("권한 없는 사용자는 403, 토큰 없으면 401")
    void security() throws Exception {
        mockMvc.perform(get("/api/v1/batch-jobs"))
                .andExpect(status().isUnauthorized());

        String viewerToken = "Bearer " + jwtTokenProvider.generateToken("viewer", "Viewer", "VIEWER", List.of(), null);
        mockMvc.perform(get("/api/v1/batch-jobs").header("Authorization", viewerToken))
                .andExpect(status().isForbidden());
    }

    /**
     * Chunk Job 수동 실행 시 장기 미접속 사용자만 잠기고 실행내역이 COMPLETED로 기록되는지 검증한다.
     * @throws Exception MockMvc 호출 또는 실행 대기 중 오류가 발생한 경우
     */
    @Test
    @DisplayName("수동 실행(Chunk Job): 장기 미접속 사용자만 잠기고 실행내역이 COMPLETED로 기록된다")
    void manualExecutionOfChunkJob() throws Exception {
        mockMvc.perform(post("/api/v1/batch-jobs/BHFL003/executions")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"params\":{\"idleDays\":\"30\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        JsonNode execution = waitForFinishedExecution("BHFL003");
        assertThat(execution.path("execStatCd").asText()).isEqualTo("COMPLETED");
        assertThat(execution.path("execClsCd").asText()).isEqualTo("M");
        assertThat(execution.path("firsRegrId").asText()).isEqualTo("admin");
        assertThat(execution.path("procCnt").asLong()).isEqualTo(2);
        assertThat(execution.path("succCnt").asLong()).isEqualTo(2);

        Map<String, String> lockYn = Map.of(
                "U001", queryLockYn("U001"),
                "U002", queryLockYn("U002"),
                "U003", queryLockYn("U003"),
                "U004", queryLockYn("U004"));
        assertThat(lockYn).containsEntry("U001", "Y")
                .containsEntry("U002", "Y")
                .containsEntry("U003", "N")
                .containsEntry("U004", "N");

        // 실행 상세: JobParameters와 Step 처리 현황
        mockMvc.perform(get("/api/v1/batch-executions/" + execution.path("dsptExecSn").asLong())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobParams.idleDays").value("30"))
                .andExpect(jsonPath("$.data.jobParams.jobId").value("BHFL003"))
                .andExpect(jsonPath("$.data.steps[0].stepName").value("inactiveUserLockStep"))
                .andExpect(jsonPath("$.data.steps[0].writeCount").value(2));

        // 배치직무 목록: 최종 실행내역과 최종 실행상태 조건
        mockMvc.perform(get("/api/v1/batch-jobs")
                        .param("lastExecStatCd", "COMPLETED")
                        .param("keyword", "BHFL003")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].lastDsptExecSn").value(execution.path("dsptExecSn").asLong()))
                .andExpect(jsonPath("$.data[0].lastExecStatCd").value("COMPLETED"))
                .andExpect(jsonPath("$.data[0].lastSuccCnt").value(2));
    }

    /**
     * Tasklet Job(실행이력 정리) 수동 실행 시 실행내역이 COMPLETED로 기록되는지 검증한다.
     * @throws Exception MockMvc 호출 또는 실행 대기 중 오류가 발생한 경우
     */
    @Test
    @DisplayName("수동 실행(Tasklet Job): 실행이력 정리 Job이 COMPLETED로 기록된다")
    void manualExecutionOfTaskletJob() throws Exception {
        mockMvc.perform(post("/api/v1/batch-jobs/BHFL002/executions")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());

        JsonNode execution = waitForFinishedExecution("BHFL002");
        assertThat(execution.path("execStatCd").asText()).isEqualTo("COMPLETED");
    }

    /**
     * 스케줄 일시정지 후 배치직무를 수정해도 PAUSED 상태가 유지되고, 재개하면 NORMAL이 되는지 검증한다.
     * @throws Exception MockMvc 호출 중 오류가 발생한 경우
     */
    @Test
    @DisplayName("스케줄 일시정지 후 배치직무를 수정해도 PAUSED 상태가 유지되고, 재개하면 NORMAL")
    void pauseAndResumeSchedule() throws Exception {
        mockMvc.perform(patch("/api/v1/batch-jobs/BHFL002/schedule")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schedStatus\":\"PAUSED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/batch-jobs/BHFL002")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jobId":"BHFL002","jobNm":"배치 실행이력 정리","wrkMdulCd":"SY",
                                 "execClassNm":"kr.or.coder.batch.job.system.execlogpurge.ExecLogPurgeJobConfig",
                                 "scheExpr":"0 0 4 * * ?","msfrPlcyCd":"01","mltExecYn":"N",
                                 "jobParmCont":"{\\"retentionDays\\":\\"90\\"}","useYn":"Y"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/batch-jobs/BHFL002").header("Authorization", adminToken))
                .andExpect(jsonPath("$.data.scheExpr").value("0 0 4 * * ?"))
                .andExpect(jsonPath("$.data.schedStatus").value("PAUSED"));

        mockMvc.perform(patch("/api/v1/batch-jobs/BHFL002/schedule")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schedStatus\":\"NORMAL\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/batch-jobs/BHFL002").header("Authorization", adminToken))
                .andExpect(jsonPath("$.data.schedStatus").value("NORMAL"));
    }

    /**
     * 미등록 Job 빈 400, 잘못된 cron 400, 없는 배치직무 404 오류 응답을 검증한다.
     * @throws Exception MockMvc 호출 중 오류가 발생한 경우
     */
    @Test
    @DisplayName("검증 오류: 배치ID 형식 400, 미등록 Job 빈 400, 잘못된 cron 400, 없는 배치직무 404")
    void validationErrors() throws Exception {
        mockMvc.perform(post("/api/v1/batch-jobs")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobId\":\"execLogPurgeJob\",\"jobNm\":\"x\",\"wrkMdulCd\":\"SY\",\"useYn\":\"Y\","
                                + "\"execClassNm\":\"kr.or.coder.batch.job.system.execlogpurge.ExecLogPurgeJobConfig\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/batch-jobs")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobId\":\"BHFL099\",\"jobNm\":\"x\",\"wrkMdulCd\":\"SY\",\"useYn\":\"Y\","
                                + "\"execClassNm\":\"kr.or.coder.batch.job.system.UnknownJobConfig\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("sy.batch.job.bean.not.registered"));

        mockMvc.perform(post("/api/v1/batch-jobs")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobId\":\"BHFL002\",\"jobNm\":\"x\",\"wrkMdulCd\":\"SY\","
                                + "\"execClassNm\":\"kr.or.coder.batch.job.system.execlogpurge.ExecLogPurgeJobConfig\","
                                + "\"scheExpr\":\"every day\",\"useYn\":\"Y\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("sy.batch.schedule.invalid.expression"));

        mockMvc.perform(get("/api/v1/batch-jobs/notExistJob").header("Authorization", adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("sy.batch.job.not.found"));
    }

    /**
     * 해당 직무의 최신 실행내역에 종료일시가 기록될 때까지 대기한다.
     * @param jobId 배치직무 ID
     * @return 종료된 실행내역 JSON
     * @throws Exception MockMvc 호출 또는 대기 중 오류가 발생한 경우
     * @throws AssertionError 제한 시간 안에 실행이 종료되지 않은 경우
     */
    private JsonNode waitForFinishedExecution(String jobId) throws Exception {
        long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            String body = mockMvc.perform(get("/api/v1/batch-executions")
                            .param("jobId", jobId)
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode list = objectMapper.readTree(body).path("data").path("list");
            if (!list.isEmpty() && !list.get(0).path("execEndDttm").isMissingNode()
                    && !list.get(0).path("execEndDttm").isNull()) {
                return list.get(0);
            }
            Thread.sleep(200);
        }
        throw new AssertionError("batch execution did not finish in time. jobId=" + jobId);
    }

    /**
     * 사용자의 잠금여부(USR_LOCK_YN)를 조회한다.
     * @param usrId 사용자 ID
     * @return 잠금여부 (값이 없으면 N)
     */
    private String queryLockYn(String usrId) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(USR_LOCK_YN, 'N') FROM TB_USR_M WHERE USR_ID = ?", String.class, usrId);
    }
}
