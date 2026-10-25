package kr.or.coder.batch.common.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.or.coder.batch.common.exception.BatchException;
import kr.or.coder.core.common.util.MessageUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.context.support.StaticMessageSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JobParametersFactory 단위 테스트
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
class JobParametersFactoryTest {

    private final JobParametersFactory factory = new JobParametersFactory(new ObjectMapper());

    /**
     * BatchException 메시지 조회에 필요한 MessageUtil을 초기화한다.
     */
    @BeforeAll
    static void initMessageUtil() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.setUseCodeAsDefaultMessage(true);
        new MessageUtil(messageSource);
    }

    /**
     * 수동 실행 파라미터가 기본 파라미터보다 우선하고 실행 정보가 non-identifying으로 들어가는지 검증한다.
     */
    @Test
    @DisplayName("수동 실행 파라미터가 배치직무 기본 파라미터보다 우선하고, 실행 정보는 non-identifying으로 들어간다")
    void createJobParameters() {
        JobParameters params = factory.createJobParameters(
                "BHFL002",
                "{\"baseDt\":\"20261001\",\"retentionDays\":\"90\"}",
                "{\"baseDt\":\"20261008\"}",
                "fire-001",
                BatchConstants.EXEC_CLS_MANUAL,
                "admin",
                "node-1");

        assertThat(params.getString("baseDt")).isEqualTo("20261008");
        assertThat(params.getString("retentionDays")).isEqualTo("90");
        assertThat(params.getString(BatchConstants.PARM_FIRE_ID)).isEqualTo("fire-001");
        assertThat(params.getString(BatchConstants.PARM_REQUEST_DTTM)).hasSize(17);
        assertThat(params.getParameter("baseDt").isIdentifying()).isTrue();

        JobParameter<?> execClsCd = params.getParameter(BatchConstants.PARM_EXEC_CLS_CD);
        assertThat(execClsCd.getValue()).isEqualTo("M");
        assertThat(execClsCd.isIdentifying()).isFalse();
        assertThat(params.getParameter(BatchConstants.PARM_REQ_USR_ID).isIdentifying()).isFalse();
        assertThat(params.getParameter(BatchConstants.PARM_EXEC_SRV_NM).isIdentifying()).isFalse();

        JobParameter<?> jobId = params.getParameter(BatchConstants.PARM_JOB_ID);
        assertThat(jobId.getValue()).isEqualTo("BHFL002");
        assertThat(jobId.isIdentifying()).isFalse();
    }

    /**
     * 재시작 파라미터가 이전 identifying 파라미터를 유지하고 실행 정보만 교체하는지 검증한다.
     */
    @Test
    @DisplayName("재시작 파라미터는 이전 identifying 파라미터를 유지하고 실행 정보만 교체한다")
    void createRestartJobParameters() {
        JobParameters previous = new JobParametersBuilder()
                .addString(BatchConstants.PARM_REQUEST_DTTM, "20261008030000000")
                .addString("baseDt", "20261008")
                .addString(BatchConstants.PARM_EXEC_CLS_CD, BatchConstants.EXEC_CLS_SCHEDULE, false)
                .addString(BatchConstants.PARM_REQ_USR_ID, "BATCH", false)
                .toJobParameters();

        JobParameters restart = factory.createRestartJobParameters(
                previous, "BHFL002", BatchConstants.EXEC_CLS_RESTART, "admin", "node-2");

        assertThat(restart.getIdentifyingParameters()).isEqualTo(previous.getIdentifyingParameters());
        assertThat(restart.getString(BatchConstants.PARM_EXEC_CLS_CD)).isEqualTo("R");
        assertThat(restart.getString(BatchConstants.PARM_REQ_USR_ID)).isEqualTo("admin");
        assertThat(restart.getString(BatchConstants.PARM_EXEC_SRV_NM)).isEqualTo("node-2");
        assertThat(restart.getString(BatchConstants.PARM_JOB_ID)).isEqualTo("BHFL002");
    }

    /**
     * JSON 객체가 아닌 파라미터는 BatchException이 발생하고 빈 값은 빈 Map이 되는지 검증한다.
     */
    @Test
    @DisplayName("JSON 객체가 아닌 파라미터는 BatchException")
    void parseParametersInvalid() {
        assertThatThrownBy(() -> factory.parseParameters("baseDt=20261008"))
                .isInstanceOf(BatchException.class);
        assertThat(factory.parseParameters(null)).isEmpty();
        assertThat(factory.parseParameters(" ")).isEmpty();
    }
}
