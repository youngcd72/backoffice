package kr.or.coder.batch.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 배치 모듈 설정 프로퍼티 (coder.batch.*)
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
@Getter
@Setter
@ConfigurationProperties(prefix = "coder.batch")
public class BatchProperties {

    /**
     * 스케줄 실행 시 감사 컬럼(FIRS_REGR_ID / FINA_REGR_ID)에 기록할 시스템 사용자 ID.
     */
    private String systemUserId = "BATCH";

    /**
     * 실행 이력(BATCH_* 메타, TB_DSPT_EXEC_L) 보관 기간(일).
     */
    private int retentionDays = 90;

    /**
     * Chunk 기본 커밋 단위.
     */
    private int defaultChunkSize = 1000;

    /**
     * CronTrigger 시간대.
     */
    private String timeZone = "Asia/Seoul";
}
