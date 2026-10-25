package kr.or.coder.batch.job.system.inactiveuserlock;

import kr.or.coder.batch.common.config.BatchProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 장기 미접속 사용자에 잠금일시와 수정자 정보를 설정하는 Processor
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
public class InactiveUserLockProcessor implements ItemProcessor<InactiveUser, InactiveUser> {

    private final BatchProperties batchProperties;

    /**
     * 사용자 잠금일시를 현재 시각으로, 최종등록자ID를 시스템 사용자 ID로 설정한다.
     * @param item 잠금 대상 사용자
     * @return 잠금 정보가 설정된 사용자
     */
    @Override
    public InactiveUser process(InactiveUser item) {
        item.setUsrLockDttm(LocalDateTime.now());
        item.setFinaRegrId(batchProperties.getSystemUserId());
        return item;
    }
}
