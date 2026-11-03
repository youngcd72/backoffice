package kr.or.coder.biz.system.link.mapper;

import kr.or.coder.biz.system.link.dto.LinkOrgInterfaceResponse;
import kr.or.coder.biz.system.link.dto.LinkOrgRequest;
import kr.or.coder.biz.system.link.dto.LinkOrgResponse;
import kr.or.coder.biz.system.link.dto.LinkOrgSaveRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SYLK010 연계기관·기관별 허용 인터페이스 MyBatis Mapper (TB_LNK_ORG_M, TB_LNK_ORG_INFC_R)
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
public interface LinkOrgMapper {

    /**
     * 조회 조건에 맞는 연계기관 목록을 기관ID 순으로 조회 (사용 가능 키 수 포함)
     * @param request 조회 조건 (기관ID·기관명 부분 일치, 채널구분코드, 사용여부)
     * @return 연계기관 목록
     */
    List<LinkOrgResponse> selectLinkOrgList(LinkOrgRequest request);

    /**
     * 연계기관 1건을 조회
     * @param lnkOrgId 연계기관ID
     * @return 연계기관 (없으면 null)
     */
    LinkOrgResponse selectLinkOrg(@Param("lnkOrgId") String lnkOrgId);

    /**
     * 연계기관ID가 등록되어 있는지 판별
     * @param lnkOrgId 연계기관ID
     * @return 등록되어 있으면 true
     */
    boolean existsLinkOrg(@Param("lnkOrgId") String lnkOrgId);

    /**
     * 연계기관을 등록
     * @param item 연계기관
     * @param usrId 등록/수정 사용자ID
     * @return 등록 건수
     */
    int insertLinkOrg(@Param("item") LinkOrgSaveRequest item, @Param("usrId") String usrId);

    /**
     * 연계기관을 수정
     * @param item 연계기관
     * @param usrId 수정 사용자ID
     * @return 수정 건수
     */
    int updateLinkOrg(@Param("item") LinkOrgSaveRequest item, @Param("usrId") String usrId);

    /**
     * 전체 인터페이스를 인터페이스ID 순으로 조회하고 연계기관의 허용 여부를 붙인다
     * @param lnkOrgId 연계기관ID
     * @return 전체 인터페이스 + 허용 여부
     */
    List<LinkOrgInterfaceResponse> selectLinkOrgInterfaceList(@Param("lnkOrgId") String lnkOrgId);

    /**
     * 연계기관에 인터페이스를 허용 (관계가 없으면 등록, 있으면 사용여부를 Y로 수정)
     * @param lnkOrgId 연계기관ID
     * @param infcId 인터페이스ID
     * @param usrId 등록/수정 사용자ID
     * @return 처리 건수
     */
    int insertLinkOrgInterface(@Param("lnkOrgId") String lnkOrgId, @Param("infcId") String infcId,
                               @Param("usrId") String usrId);

    /**
     * 목록에 없는 기존 허용 인터페이스를 사용 안 함(N)으로 수정 (행은 지우지 않음)
     * @param lnkOrgId 연계기관ID
     * @param infcIdList 계속 허용할 인터페이스ID 목록 (비어 있으면 모두 N)
     * @param usrId 수정 사용자ID
     * @return 수정 건수
     */
    int updateLinkOrgInterfaceUnused(@Param("lnkOrgId") String lnkOrgId, @Param("infcIdList") List<String> infcIdList,
                                     @Param("usrId") String usrId);
}
