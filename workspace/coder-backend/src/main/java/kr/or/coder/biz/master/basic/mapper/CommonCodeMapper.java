package kr.or.coder.biz.master.basic.mapper;

import kr.or.coder.biz.master.basic.dto.CommonCodeDetailItemRequest;
import kr.or.coder.biz.master.basic.dto.CommonCodeDetailResponse;
import kr.or.coder.biz.master.basic.dto.CommonCodeItemRequest;
import kr.or.coder.biz.master.basic.dto.CommonCodeRequest;
import kr.or.coder.biz.master.basic.dto.CommonCodeResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MSBS010 공통코드관리 MyBatis Mapper (TB_COMM_CD_M, TB_COMM_CD_D)
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
public interface CommonCodeMapper {

    /**
     * 조회 조건에 맞는 공통코드 분류 목록을 업무모듈·정렬순번 순으로 조회
     * @param request 조회 조건 (업무모듈코드, 분류코드, 분류명)
     * @return 공통코드 분류 목록
     */
    List<CommonCodeResponse> selectCommonCodeList(CommonCodeRequest request);

    /**
     * 공통분류의 상세코드 목록을 정렬순번 순으로 조회 (useYn=Y 이면 사용 중인 분류의 사용 중인 코드만)
     * @param wrkMdulCd 업무모듈코드
     * @param commCtgCd 공통분류코드
     * @param useYn 사용여부 조건 (Y: 화면 콤보용, 없으면 전체: 공통코드관리용)
     * @return 공통코드 상세 목록
     */
    List<CommonCodeDetailResponse> selectCommonCodeDetailList(@Param("wrkMdulCd") String wrkMdulCd,
                                                              @Param("commCtgCd") String commCtgCd,
                                                              @Param("useYn") String useYn);

    /**
     * 공통코드 분류가 등록되어 있는지 판별
     * @param wrkMdulCd 업무모듈코드
     * @param commCtgCd 공통분류코드
     * @return 등록되어 있으면 true
     */
    boolean existsCommonCode(@Param("wrkMdulCd") String wrkMdulCd, @Param("commCtgCd") String commCtgCd);

    /**
     * 공통코드 분류를 등록
     * @param item 공통코드 분류 1건
     * @param usrId 등록/수정 사용자ID
     * @return 등록 건수
     */
    int insertCommonCode(@Param("item") CommonCodeItemRequest item, @Param("usrId") String usrId);

    /**
     * 공통코드 분류를 수정
     * @param item 공통코드 분류 1건
     * @param usrId 등록/수정 사용자ID
     * @return 수정 건수
     */
    int updateCommonCode(@Param("item") CommonCodeItemRequest item, @Param("usrId") String usrId);

    /**
     * 공통코드 분류를 삭제
     * @param wrkMdulCd 업무모듈코드
     * @param commCtgCd 공통분류코드
     * @return 삭제 건수
     */
    int deleteCommonCode(@Param("wrkMdulCd") String wrkMdulCd, @Param("commCtgCd") String commCtgCd);

    /**
     * 공통코드 상세가 등록되어 있는지 판별 (상세코드가 없으면 분류에 코드가 1건이라도 있는지)
     * @param wrkMdulCd 업무모듈코드
     * @param commCtgCd 공통분류코드
     * @param commCtgDtlCd 공통분류상세코드 (null 이면 분류 전체)
     * @return 등록되어 있으면 true
     */
    boolean existsCommonCodeDetail(@Param("wrkMdulCd") String wrkMdulCd, @Param("commCtgCd") String commCtgCd,
                                   @Param("commCtgDtlCd") String commCtgDtlCd);

    /**
     * 공통코드 상세를 등록
     * @param item 공통코드 상세 1건
     * @param usrId 등록/수정 사용자ID
     * @return 등록 건수
     */
    int insertCommonCodeDetail(@Param("item") CommonCodeDetailItemRequest item, @Param("usrId") String usrId);

    /**
     * 공통코드 상세를 수정
     * @param item 공통코드 상세 1건
     * @param usrId 등록/수정 사용자ID
     * @return 수정 건수
     */
    int updateCommonCodeDetail(@Param("item") CommonCodeDetailItemRequest item, @Param("usrId") String usrId);
}
