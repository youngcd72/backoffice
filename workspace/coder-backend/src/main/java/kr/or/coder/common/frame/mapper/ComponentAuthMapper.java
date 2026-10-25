package kr.or.coder.common.frame.mapper;

import kr.or.coder.common.frame.dto.auth.ComponentAuthResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 로그인 사용자의 컴포넌트 권한(TB_PRGM_CMPN_M, TB_AUTH_GRP_CMPN_R, TB_USR_CMPN_R) MyBatis Mapper
 * @author ywkim
 * @since 2026.10.19
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.19  ywkim        최초 생성
 * </pre>
 */
@Mapper
public interface ComponentAuthMapper {

    /**
     * 프로그램의 사용 중인 컴포넌트별로 사용자의 권한여부를 조회 (적용기간 내 권한그룹 또는 사용자 권한 중 하나라도 Y 이면 Y)
     * @param usrId 사용자ID
     * @param prgmId 프로그램ID
     * @return 컴포넌트 권한 목록
     */
    List<ComponentAuthResponse> selectComponentAuthList(@Param("usrId") String usrId, @Param("prgmId") String prgmId);
}
