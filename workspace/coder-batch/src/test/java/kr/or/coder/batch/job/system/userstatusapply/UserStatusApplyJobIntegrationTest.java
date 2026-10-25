package kr.or.coder.batch.job.system.userstatusapply;

import kr.or.coder.batch.common.support.BatchConstants;
import kr.or.coder.batch.support.TestSchemaInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * userStatusApplyJob(BHFL001 사용자상태변경 적용) 통합 테스트.
 * PostgreSQL 임시 스키마(TestSchemaInitializer)에 테스트 사용자를 넣고 Job을 실행해 TB_USR_M 반영 결과를 검증한다.
 * <pre>./gradlew integrationTest</pre>
 * @author ywkim
 * @since 2026.10.17
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.17  ywkim        최초 생성
 * </pre>
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestSchemaInitializer.class)
class UserStatusApplyJobIntegrationTest {

    private static final String BASE_DT = "20261017";

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier(UserStatusApplyJobConfig.JOB_NAME)
    private Job userStatusApplyJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 각 테스트 전에 상태별 테스트 사용자를 다시 등록한다.
     */
    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM TB_USR_M WHERE USR_ID LIKE 'S1%'");
        insertUser("S101", "R", "Y", "20261015", "99991231");   // 퇴직 → N, 사용종료일자 = 퇴사일자
        insertUser("S102", "R", "Y", null, "99991231");         // 퇴직, 퇴사일자 없음 → N, 사용종료일자 = 기준일자
        insertUser("S103", "L", "Y", null, "99991231");         // 휴직 → N, 사용종료일자 유지
        insertUser("S104", "T", "Y", null, "99991231");         // 재직 → 변경 없음
        insertUser("S105", "R", "N", "20261001", "20261001");   // 이미 반영된 퇴직 → 변경 없음
    }

    /**
     * 퇴직·휴직 사용자만 사용여부가 N이 되고, 퇴직 사용자의 사용종료일자가 퇴사일자(없으면 기준일자)로 맞춰지는지 검증한다.
     * @throws Exception Job 실행 중 오류가 발생한 경우
     */
    @Test
    @DisplayName("퇴직·휴직 사용자는 사용여부 N, 퇴직 사용자는 사용종료일자를 퇴사일자로 맞춘다")
    void applyUserStatus() throws Exception {
        JobExecution jobExecution = runJob();

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(writeCount(jobExecution)).isEqualTo(3);

        assertUser("S101", "N", "20261015", "BATCH");
        assertUser("S102", "N", BASE_DT, "BATCH");
        assertUser("S103", "N", "99991231", "BATCH");
        assertUser("S104", "Y", "99991231", "TEST");
        assertUser("S105", "N", "20261001", "TEST");
    }

    /**
     * 같은 기준일자로 다시 실행하면 갱신 대상이 없는지(멱등) 검증한다.
     * @throws Exception Job 실행 중 오류가 발생한 경우
     */
    @Test
    @DisplayName("다시 실행하면 이미 반영된 사용자는 갱신하지 않는다")
    void rerunIsIdempotent() throws Exception {
        runJob();
        JobExecution secondExecution = runJob();

        assertThat(secondExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(writeCount(secondExecution)).isZero();
    }

    /**
     * 기준일자를 지정해 userStatusApplyJob을 실행한다.
     * @return Job 실행 결과
     * @throws Exception Job 실행 중 오류가 발생한 경우
     */
    private JobExecution runJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString(BatchConstants.PARM_REQUEST_DTTM, String.valueOf(System.nanoTime()))
                .addString("baseDt", BASE_DT)
                .addString(BatchConstants.PARM_EXEC_CLS_CD, BatchConstants.EXEC_CLS_MANUAL, false)
                .addString(BatchConstants.PARM_REQ_USR_ID, "TEST", false)
                .toJobParameters();
        return jobLauncher.run(userStatusApplyJob, params);
    }

    /**
     * Job 실행의 Step 쓰기 건수 합계를 구한다.
     * @param jobExecution Job 실행 결과
     * @return 쓰기 건수 합계
     */
    private long writeCount(JobExecution jobExecution) {
        return jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getWriteCount).sum();
    }

    /**
     * 테스트 사용자를 등록한다.
     * @param usrId 사용자 ID
     * @param usrStatCd 사용자상태코드
     * @param useYn 사용여부
     * @param resiDt 퇴사일자
     * @param useEndDt 사용종료일자
     */
    private void insertUser(String usrId, String usrStatCd, String useYn, String resiDt, String useEndDt) {
        jdbcTemplate.update("""
                INSERT INTO TB_USR_M (USR_ID, USR_NM, USR_STAT_CD, USE_YN, RESI_DT, USE_END_DT, FIRS_REGR_ID, FINA_REGR_ID)
                VALUES (?, ?, ?, ?, ?, ?, 'TEST', 'TEST')
                """, usrId, usrId, usrStatCd, useYn, resiDt, useEndDt);
    }

    /**
     * 사용자의 사용여부·사용종료일자·최종등록자ID를 검증한다.
     * @param usrId 사용자 ID
     * @param useYn 기대 사용여부
     * @param useEndDt 기대 사용종료일자
     * @param finaRegrId 기대 최종등록자ID
     */
    private void assertUser(String usrId, String useYn, String useEndDt, String finaRegrId) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT USE_YN, USE_END_DT, FINA_REGR_ID FROM TB_USR_M WHERE USR_ID = ?", usrId);
        assertThat(row).as(usrId)
                .containsEntry("use_yn", useYn)
                .containsEntry("use_end_dt", useEndDt)
                .containsEntry("fina_regr_id", finaRegrId);
    }
}
