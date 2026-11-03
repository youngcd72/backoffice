package kr.or.coder.biz.system.link.mapper;

import kr.or.coder.biz.system.link.dto.InterfaceCallRequest;
import kr.or.coder.biz.system.link.dto.InterfaceCallResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SYLK010 호출이력 MyBatis Mapper (TB_INFC_CALL_L, 요청일시 월 파티션)
 * @author ywkim
 * @since 2026.11.03
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.11.03  ywkim        최초 생성
 * </pre>
 */
@Mapper
public interface InterfaceCallMapper {

    /**
     * 조회 조건에 맞는 호출이력 건수를 조회
     * @param request 조회 조건 (요청일자 범위 필수)
     * @param stDttm 요청일시 시작 (포함)
     * @param endDttm 요청일시 끝 (미포함)
     * @return 호출이력 건수
     */
    int selectInterfaceCallCount(@Param("request") InterfaceCallRequest request,
                                 @Param("stDttm") LocalDateTime stDttm, @Param("endDttm") LocalDateTime endDttm);

    /**
     * 조회 조건에 맞는 호출이력을 요청일시 최근 순으로 최대 건수까지 조회 (본문 제외)
     * @param request 조회 조건 (요청일자 범위 필수)
     * @param stDttm 요청일시 시작 (포함)
     * @param endDttm 요청일시 끝 (미포함)
     * @param size 최대 조회 건수
     * @return 호출이력 목록
     */
    List<InterfaceCallResponse> selectInterfaceCallList(@Param("request") InterfaceCallRequest request,
                                                        @Param("stDttm") LocalDateTime stDttm,
                                                        @Param("endDttm") LocalDateTime endDttm,
                                                        @Param("size") int size);

    /**
     * 호출이력 1건을 요청·응답 본문까지 조회
     * 화면에는 요청일시를 밀리초까지만 주므로 그 1밀리초 범위로 찾는다 (요청일시는 파티션 키)
     * @param callSn 호출일련번호
     * @param stDttm 요청일시 시작 (포함, 밀리초)
     * @param endDttm 요청일시 끝 (미포함, 시작 + 1밀리초)
     * @return 호출이력 (없으면 null)
     */
    InterfaceCallResponse selectInterfaceCall(@Param("callSn") long callSn, @Param("stDttm") LocalDateTime stDttm,
                                              @Param("endDttm") LocalDateTime endDttm);
}
