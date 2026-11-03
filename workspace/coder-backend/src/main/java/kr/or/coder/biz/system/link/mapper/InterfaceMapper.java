package kr.or.coder.biz.system.link.mapper;

import kr.or.coder.biz.system.link.dto.InterfaceRequest;
import kr.or.coder.biz.system.link.dto.InterfaceResponse;
import kr.or.coder.biz.system.link.dto.InterfaceSaveRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SYLK010 인터페이스 MyBatis Mapper (TB_INFC_M)
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
public interface InterfaceMapper {

    /**
     * 조회 조건에 맞는 인터페이스 목록을 인터페이스ID 순으로 조회 (허용 기관 수 포함)
     * @param request 조회 조건 (인터페이스ID·명·URL 부분 일치, 사용여부)
     * @return 인터페이스 목록
     */
    List<InterfaceResponse> selectInterfaceList(InterfaceRequest request);

    /**
     * 인터페이스ID가 등록되어 있는지 판별
     * @param infcId 인터페이스ID
     * @return 등록되어 있으면 true
     */
    boolean existsInterface(@Param("infcId") String infcId);

    /**
     * 인터페이스ID 목록 중 등록된 인터페이스 수를 조회
     * @param infcIdList 인터페이스ID 목록 (비어 있지 않아야 함)
     * @return 등록된 인터페이스 수
     */
    int selectInterfaceCount(@Param("infcIdList") List<String> infcIdList);

    /**
     * 인터페이스를 등록
     * @param item 인터페이스
     * @param usrId 등록/수정 사용자ID
     * @return 등록 건수
     */
    int insertInterface(@Param("item") InterfaceSaveRequest item, @Param("usrId") String usrId);

    /**
     * 인터페이스를 수정
     * @param item 인터페이스
     * @param usrId 수정 사용자ID
     * @return 수정 건수
     */
    int updateInterface(@Param("item") InterfaceSaveRequest item, @Param("usrId") String usrId);
}
