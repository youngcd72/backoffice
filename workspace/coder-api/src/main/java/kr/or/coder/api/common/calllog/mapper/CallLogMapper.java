package kr.or.coder.api.common.calllog.mapper;

import kr.or.coder.api.common.calllog.CallLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 인터페이스 호출 이력 Mapper (TB_INFC_CALL_L)
 * @author ywkim
 * @since 2026.10.27
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.27  ywkim        최초 생성
 * </pre>
 */
@Mapper
public interface CallLogMapper {

    /**
     * 인터페이스 호출 이력을 한 번에 여러 건 등록한다.
     * @param callLogList 호출 이력 목록
     * @return 등록 건수
     */
    int insertCallLogList(@Param("callLogList") List<CallLog> callLogList);
}
