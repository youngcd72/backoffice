package kr.or.coder.batch.frame.service;

import kr.or.coder.batch.common.config.BatchProperties;
import kr.or.coder.batch.common.exception.BatchException;
import kr.or.coder.batch.common.support.BatchJobLocator;
import kr.or.coder.batch.common.support.JobParametersFactory;
import kr.or.coder.batch.frame.dto.BatchJobRequest;
import kr.or.coder.batch.frame.entity.BatchJob;
import kr.or.coder.batch.frame.mapper.BatchAlarmMapper;
import kr.or.coder.batch.frame.mapper.BatchExecutionMapper;
import kr.or.coder.batch.frame.mapper.BatchJobMapper;
import kr.or.coder.core.common.util.MessageUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.context.support.StaticMessageSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BatchJobServiceImpl 배치직무 저장(신규 등록/수정) 단위 테스트
 * @author ywkim
 * @since 2026.10.23
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.23  ywkim        최초 생성 (신규 등록 시 배치ID 중복 검증)
 * </pre>
 */
class BatchJobServiceImplTest {

    private static final String JOB_ID = "BHFL001";

    private BatchJobMapper batchJobMapper;
    private ScheduleService scheduleService;
    private BatchJobServiceImpl batchJobService;

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
     * 의존 객체를 mock으로 만들어 테스트 대상 Service를 생성한다.
     */
    @BeforeEach
    void setUp() {
        batchJobMapper = mock(BatchJobMapper.class);
        scheduleService = mock(ScheduleService.class);
        batchJobService = new BatchJobServiceImpl(
                batchJobMapper,
                mock(BatchExecutionMapper.class),
                mock(BatchAlarmMapper.class),
                scheduleService,
                mock(BatchJobLocator.class),
                mock(JobExplorer.class),
                mock(JobParametersFactory.class),
                mock(BatchProperties.class));
    }

    /**
     * 신규 등록인데 이미 있는 배치ID면 중복 오류를 던지고 저장·스케줄 반영을 하지 않는지 검증한다.
     */
    @Test
    @DisplayName("신규 등록 시 이미 있는 배치ID면 sy.batch.job.duplicate 오류, 기존 배치를 덮어쓰지 않는다")
    void saveBatchJobDuplicate() {
        when(batchJobMapper.selectBatchJob(JOB_ID)).thenReturn(BatchJob.builder().jobId(JOB_ID).wrkMdulCd("FL").build());

        assertThatThrownBy(() -> batchJobService.saveBatchJob(createRequest(), true))
                .isInstanceOf(BatchException.class)
                .hasFieldOrPropertyWithValue("code", "sy.batch.job.duplicate");

        verify(batchJobMapper, never()).insertBatchJob(any());
        verify(batchJobMapper, never()).updateBatchJob(any());
        verify(scheduleService, never()).saveSchedule(any(), any());
    }

    /**
     * 신규 등록이고 없는 배치ID면 insert 되는지 검증한다.
     */
    @Test
    @DisplayName("신규 등록 시 없는 배치ID면 insert 한다")
    void saveBatchJobInsert() {
        when(batchJobMapper.selectBatchJob(JOB_ID)).thenReturn(null);

        batchJobService.saveBatchJob(createRequest(), true);

        verify(batchJobMapper).insertBatchJob(any());
        verify(batchJobMapper, never()).updateBatchJob(any());
    }

    /**
     * 수정 요청이고 있는 배치ID면 update 되는지 검증한다.
     */
    @Test
    @DisplayName("수정 시 있는 배치ID면 update 한다")
    void saveBatchJobUpdate() {
        when(batchJobMapper.selectBatchJob(JOB_ID)).thenReturn(BatchJob.builder().jobId(JOB_ID).wrkMdulCd("FL").build());

        batchJobService.saveBatchJob(createRequest(), false);

        verify(batchJobMapper).updateBatchJob(any());
        verify(batchJobMapper, never()).insertBatchJob(any());
    }

    /**
     * 테스트용 배치직무 저장 요청을 만든다.
     * @return 배치직무 저장 요청
     */
    private BatchJobRequest createRequest() {
        return BatchJobRequest.builder()
                .jobId(JOB_ID)
                .jobNm("테스트 배치")
                .execClassNm("kr.or.coder.batch.job.TestJobConfig")
                .wrkMdulCd("FL")
                .useYn("N")
                .build();
    }
}
