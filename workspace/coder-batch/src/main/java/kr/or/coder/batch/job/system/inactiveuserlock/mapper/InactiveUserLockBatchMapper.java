package kr.or.coder.batch.job.system.inactiveuserlock.mapper;

import kr.or.coder.batch.job.system.inactiveuserlock.InactiveUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.cursor.Cursor;

import java.time.LocalDateTime;

/**
 * inactiveUserLockJob 전용 Mapper. Reader/Writer는 statement ID(namespace + id)로 호출한다.
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
@Mapper
public interface InactiveUserLockBatchMapper {

    /**
     * 기준일시 이전에 마지막으로 접속한 사용 중 사용자를 Cursor로 조회한다.
     * @param baseDttm 미접속 기준일시
     * @return 장기 미접속 사용자 Cursor
     */
    Cursor<InactiveUser> selectInactiveUserList(@Param("baseDttm") LocalDateTime baseDttm);

    /**
     * 사용자를 잠금 처리한다.
     * @param inactiveUser 잠금 대상 사용자
     * @return 갱신 건수
     */
    int updateUserLock(InactiveUser inactiveUser);
}
