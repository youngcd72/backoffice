package kr.or.coder.api.common.client.mapper;

import kr.or.coder.api.common.client.LinkClient;
import kr.or.coder.api.common.client.LinkInterface;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 연계키·연계기관·인터페이스 권한 조회 Mapper (TB_LNK_KEY_M, TB_LNK_ORG_M, TB_INFC_M, TB_LNK_ORG_INFC_R)
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
public interface LinkClientMapper {

    /**
     * 키ID로 연계키와 연계기관 정보를 조회한다.
     * @param keyId 키ID (client_id)
     * @return 연계키 정보 (없으면 null)
     */
    LinkClient selectLinkClient(@Param("keyId") String keyId);

    /**
     * 인터페이스ID로 인터페이스 정보를 조회한다.
     * @param infcId 인터페이스ID
     * @return 인터페이스 정보 (없으면 null)
     */
    LinkInterface selectLinkInterface(@Param("infcId") String infcId);

    /**
     * 연계기관에 허용된 사용 중인 인터페이스ID 목록을 조회한다.
     * @param lnkOrgId 연계기관ID
     * @return 인터페이스ID 목록
     */
    List<String> selectAllowedInfcIdList(@Param("lnkOrgId") String lnkOrgId);
}
