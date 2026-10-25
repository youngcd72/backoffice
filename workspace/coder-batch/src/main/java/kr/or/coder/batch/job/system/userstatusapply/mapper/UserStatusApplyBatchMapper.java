package kr.or.coder.batch.job.system.userstatusapply.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * userStatusApplyJob 전용 Mapper
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
@Mapper
public interface UserStatusApplyBatchMapper {

    /**
     * 퇴직 사용자의 사용여부를 N으로, 사용종료일자를 퇴사일자(없으면 기준일자)로 갱신한다.
     * @param baseDt 기준일자 (yyyyMMdd)
     * @param finaRegrId 최종등록자ID
     * @return 갱신 건수
     */
    int updateRetiredUser(@Param("baseDt") String baseDt, @Param("finaRegrId") String finaRegrId);

    /**
     * 휴직 사용자의 사용여부를 N으로 갱신한다.
     * @param finaRegrId 최종등록자ID
     * @return 갱신 건수
     */
    int updateLeaveUser(@Param("finaRegrId") String finaRegrId);
}
