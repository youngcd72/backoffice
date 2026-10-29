package kr.or.coder.api.biz.system.sample.mapper;

import kr.or.coder.api.biz.system.sample.dto.SampleCodeResponse;
import kr.or.coder.api.biz.system.sample.dto.SampleResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 샘플 업무 API Mapper (TB_COMM_CD_M, TB_COMM_CD_D 조회)
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
public interface SampleMapper {

    /**
     * 사용 중인 공통코드 그룹을 조회한다.
     * @param wrkMdulCd 업무모듈코드
     * @param commCtgCd 공통분류코드
     * @return 공통코드 그룹 (없으면 null)
     */
    SampleResponse selectSample(@Param("wrkMdulCd") String wrkMdulCd, @Param("commCtgCd") String commCtgCd);

    /**
     * 공통코드 그룹의 사용 중인 상세코드 목록을 정렬순번 순으로 조회한다.
     * @param wrkMdulCd 업무모듈코드
     * @param commCtgCd 공통분류코드
     * @return 상세코드 목록
     */
    List<SampleCodeResponse> selectSampleCodeList(@Param("wrkMdulCd") String wrkMdulCd, @Param("commCtgCd") String commCtgCd);
}
