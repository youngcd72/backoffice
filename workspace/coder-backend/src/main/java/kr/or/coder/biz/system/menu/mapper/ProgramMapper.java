package kr.or.coder.biz.system.menu.mapper;

import kr.or.coder.biz.system.menu.dto.ProgramComponentItemRequest;
import kr.or.coder.biz.system.menu.dto.ProgramComponentResponse;
import kr.or.coder.biz.system.menu.dto.ProgramItemRequest;
import kr.or.coder.biz.system.menu.dto.ProgramRequest;
import kr.or.coder.biz.system.menu.dto.ProgramResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SYPG010 프로그램관리 MyBatis Mapper (TB_PRGM_M)
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
public interface ProgramMapper {

    /**
     * 조회 조건에 맞는 프로그램 목록을 정렬순번 순으로 조회
     * @param request 조회 조건 (프로그램경로코드, 프로그램ID, 프로그램명, 사용여부)
     * @return 프로그램 목록
     */
    List<ProgramResponse> selectProgramList(ProgramRequest request);

    /**
     * 프로그램의 컴포넌트 목록을 정렬순번 순으로 조회
     * @param prgmId 프로그램ID
     * @return 컴포넌트 목록
     */
    List<ProgramComponentResponse> selectProgramComponentList(@Param("prgmId") String prgmId);

    /**
     * 프로그램ID가 등록되어 있는지 판별
     * @param prgmId 프로그램ID
     * @return 등록되어 있으면 true
     */
    boolean existsProgram(@Param("prgmId") String prgmId);

    /**
     * 프로그램을 등록
     * @param item 프로그램 1건
     * @param usrId 등록/수정 사용자ID
     * @return 등록 건수
     */
    int insertProgram(@Param("item") ProgramItemRequest item, @Param("usrId") String usrId);

    /**
     * 프로그램을 수정 (정렬순번은 변경하지 않음)
     * @param item 프로그램 1건
     * @param usrId 등록/수정 사용자ID
     * @return 수정 건수
     */
    int updateProgram(@Param("item") ProgramItemRequest item, @Param("usrId") String usrId);

    /**
     * 프로그램을 삭제
     * @param prgmId 프로그램ID
     * @return 삭제 건수
     */
    int deleteProgram(@Param("prgmId") String prgmId);

    /**
     * 프로그램 컴포넌트가 등록되어 있는지 판별
     * @param prgmId 프로그램ID
     * @param cmpnId 컴포넌트ID
     * @return 등록되어 있으면 true
     */
    boolean existsProgramComponent(@Param("prgmId") String prgmId, @Param("cmpnId") String cmpnId);

    /**
     * 프로그램 컴포넌트를 등록
     * @param item 컴포넌트 1건
     * @param usrId 등록/수정 사용자ID
     * @return 등록 건수
     */
    int insertProgramComponent(@Param("item") ProgramComponentItemRequest item, @Param("usrId") String usrId);

    /**
     * 프로그램 컴포넌트를 수정
     * @param item 컴포넌트 1건
     * @param usrId 등록/수정 사용자ID
     * @return 수정 건수
     */
    int updateProgramComponent(@Param("item") ProgramComponentItemRequest item, @Param("usrId") String usrId);

    /**
     * 프로그램 컴포넌트를 삭제
     * @param prgmId 프로그램ID
     * @param cmpnId 컴포넌트ID
     * @return 삭제 건수
     */
    int deleteProgramComponent(@Param("prgmId") String prgmId, @Param("cmpnId") String cmpnId);

    /**
     * 프로그램의 컴포넌트를 모두 삭제 (프로그램 삭제 시)
     * @param prgmId 프로그램ID
     * @return 삭제 건수
     */
    int deleteProgramComponentList(@Param("prgmId") String prgmId);

    /**
     * 프로그램을 사용하는 메뉴가 있는지 판별
     * @param prgmId 프로그램ID
     * @return 메뉴가 있으면 true
     */
    boolean existsProgramMenu(@Param("prgmId") String prgmId);

    /**
     * 프로그램 컴포넌트의 권한그룹 컴포넌트 권한을 삭제 (프로그램·컴포넌트 삭제 시)
     * @param prgmId 프로그램ID
     * @param cmpnId 컴포넌트ID (null 이면 프로그램의 모든 컴포넌트)
     * @return 삭제 건수
     */
    int deleteProgramAuthGroupComponentList(@Param("prgmId") String prgmId, @Param("cmpnId") String cmpnId);

    /**
     * 프로그램 컴포넌트의 사용자 컴포넌트 권한을 삭제 (프로그램·컴포넌트 삭제 시)
     * @param prgmId 프로그램ID
     * @param cmpnId 컴포넌트ID (null 이면 프로그램의 모든 컴포넌트)
     * @return 삭제 건수
     */
    int deleteProgramUserComponentList(@Param("prgmId") String prgmId, @Param("cmpnId") String cmpnId);
}
