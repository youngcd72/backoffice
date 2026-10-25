package kr.or.coder.batch.common.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.or.coder.batch.common.exception.BatchErrorCode;
import kr.or.coder.batch.common.exception.BatchException;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Batch JobParameters 생성 규칙.
 * <ol>
 *   <li>requestDttm, fireId (identifying) - 실행마다 새 JobInstance 생성</li>
 *   <li>배치직무 기본 파라미터 JOB_PARM_CONT (identifying)</li>
 *   <li>수동 실행 파라미터 (identifying, 2보다 우선)</li>
 *   <li>jobId(배치ID), execClsCd, reqUsrId, execSrvNm (non-identifying)</li>
 * </ol>
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
@Component
@RequiredArgsConstructor
public class JobParametersFactory {

    private static final DateTimeFormatter REQUEST_DTTM_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final TypeReference<LinkedHashMap<String, String>> STRING_MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    /**
     * 신규 실행용 JobParameters를 생성한다.
     * @param jobId 배치ID (TB_DSPT_JOB_M.JOB_ID)
     * @param jobParmCont 배치직무 기본 파라미터 (JSON)
     * @param manualParmCont 수동 실행 파라미터 (JSON, 기본 파라미터보다 우선)
     * @param fireId Quartz fireInstanceId
     * @param execClsCd 실행구분코드
     * @param reqUsrId 실행 요청 사용자 ID
     * @param execSrvNm 실행 서버명
     * @return 생성된 JobParameters
     * @throws BatchException 파라미터 JSON 형식이 올바르지 않은 경우
     */
    public JobParameters createJobParameters(String jobId,
                                             String jobParmCont,
                                             String manualParmCont,
                                             String fireId,
                                             String execClsCd,
                                             String reqUsrId,
                                             String execSrvNm) {
        JobParametersBuilder builder = new JobParametersBuilder()
                .addString(BatchConstants.PARM_REQUEST_DTTM, LocalDateTime.now().format(REQUEST_DTTM_FORMAT))
                .addString(BatchConstants.PARM_FIRE_ID, fireId);

        Map<String, String> params = new LinkedHashMap<>(parseParameters(jobParmCont));
        params.putAll(parseParameters(manualParmCont));
        params.forEach(builder::addString);

        addRuntimeParameters(builder, jobId, execClsCd, reqUsrId, execSrvNm);
        return builder.toJobParameters();
    }

    /**
     * 재시작용 JobParameters: 이전 실행의 identifying 파라미터는 그대로 두고(같은 JobInstance),
     * non-identifying 실행 정보만 새로 채운다.
     * @param previous 이전 실행의 JobParameters
     * @param jobId 배치ID (TB_DSPT_JOB_M.JOB_ID)
     * @param execClsCd 실행구분코드
     * @param reqUsrId 실행 요청 사용자 ID
     * @param execSrvNm 실행 서버명
     * @return 재시작용 JobParameters
     */
    public JobParameters createRestartJobParameters(JobParameters previous,
                                                    String jobId,
                                                    String execClsCd,
                                                    String reqUsrId,
                                                    String execSrvNm) {
        JobParametersBuilder builder = new JobParametersBuilder();
        previous.getParameters().forEach((key, parameter) -> {
            if (parameter.isIdentifying()) {
                builder.addJobParameter(key, parameter);
            }
        });
        addRuntimeParameters(builder, jobId, execClsCd, reqUsrId, execSrvNm);
        return builder.toJobParameters();
    }

    /**
     * JSON 객체 문자열({"key":"value"})을 파라미터 Map으로 변환한다.
     * @param json JSON 객체 문자열
     * @return 파라미터 Map, 값이 없으면 빈 Map
     * @throws BatchException JSON 형식이 올바르지 않은 경우
     */
    public Map<String, String> parseParameters(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            Map<String, String> params = objectMapper.readValue(json, STRING_MAP_TYPE);
            return params == null ? Map.of() : params;
        } catch (JsonProcessingException e) {
            throw new BatchException(BatchErrorCode.JOB_PARAMETER_INVALID, json);
        }
    }

    /**
     * 파라미터 Map을 JSON 문자열로 변환한다.
     * @param params 파라미터 Map
     * @return JSON 문자열, 비어 있으면 null
     * @throws BatchException JSON 변환에 실패한 경우
     */
    public String toJson(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new BatchException(BatchErrorCode.JOB_PARAMETER_INVALID, String.valueOf(params));
        }
    }

    /**
     * 실행 정보(non-identifying) 파라미터를 추가한다. 값이 null인 항목은 제외한다.
     * @param builder JobParameters 빌더
     * @param jobId 배치ID
     * @param execClsCd 실행구분코드
     * @param reqUsrId 실행 요청 사용자 ID
     * @param execSrvNm 실행 서버명
     */
    private void addRuntimeParameters(JobParametersBuilder builder, String jobId, String execClsCd,
                                      String reqUsrId, String execSrvNm) {
        builder.addString(BatchConstants.PARM_JOB_ID, jobId, false);
        builder.addString(BatchConstants.PARM_EXEC_CLS_CD, execClsCd, false);
        if (reqUsrId != null) {
            builder.addString(BatchConstants.PARM_REQ_USR_ID, reqUsrId, false);
        }
        if (execSrvNm != null) {
            builder.addString(BatchConstants.PARM_EXEC_SRV_NM, execSrvNm, false);
        }
    }
}
