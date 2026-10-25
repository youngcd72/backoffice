package kr.or.coder.batch.job.system.userstatusapply;

import kr.or.coder.batch.common.config.BatchProperties;
import kr.or.coder.batch.job.system.userstatusapply.mapper.UserStatusApplyBatchMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 사용자상태변경 적용 Tasklet (퇴직·휴직 사용자 사용여부 N, 퇴직 사용자 사용종료일자를 퇴사일자로 설정)
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
@Slf4j
@Component
@StepScope
public class UserStatusApplyTasklet implements Tasklet {

    private static final DateTimeFormatter BASE_DT_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final UserStatusApplyBatchMapper userStatusApplyBatchMapper;
    private final BatchProperties batchProperties;
    private final String baseDt;

    /**
     * Tasklet을 생성한다. baseDt JobParameter가 없으면 실행일을 기준일자로 사용한다.
     * @param userStatusApplyBatchMapper 사용자상태변경 적용 Mapper
     * @param batchProperties 배치 설정 프로퍼티
     * @param baseDt 기준일자(yyyyMMdd) JobParameter, 없으면 빈 값
     * @throws java.time.format.DateTimeParseException baseDt가 yyyyMMdd 형식이 아닌 경우
     */
    public UserStatusApplyTasklet(UserStatusApplyBatchMapper userStatusApplyBatchMapper,
                                  BatchProperties batchProperties,
                                  @Value("#{jobParameters['baseDt']}") String baseDt) {
        this.userStatusApplyBatchMapper = userStatusApplyBatchMapper;
        this.batchProperties = batchProperties;
        this.baseDt = StringUtils.hasText(baseDt)
                ? LocalDate.parse(baseDt, BASE_DT_FORMAT).format(BASE_DT_FORMAT)
                : LocalDate.now().format(BASE_DT_FORMAT);
    }

    /**
     * 퇴직·휴직 사용자의 사용여부와 퇴직 사용자의 사용종료일자를 갱신하고 갱신 건수를 Step의 쓰기 건수에 반영한다.
     * @param contribution Step 처리 건수 반영 객체
     * @param chunkContext Chunk 실행 컨텍스트
     * @return 한 번 실행 후 종료하므로 RepeatStatus.FINISHED
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        String finaRegrId = batchProperties.getSystemUserId();

        int retiredCount = userStatusApplyBatchMapper.updateRetiredUser(baseDt, finaRegrId);
        int leaveCount = userStatusApplyBatchMapper.updateLeaveUser(finaRegrId);

        contribution.incrementWriteCount(retiredCount + leaveCount);
        log.info("applied user status (baseDt={}): retired {} row(s), leave {} row(s).", baseDt, retiredCount, leaveCount);
        return RepeatStatus.FINISHED;
    }
}
