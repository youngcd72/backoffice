package kr.or.coder.biz.system.logs.mapper;

import kr.or.coder.biz.system.logs.dto.LogUsrResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * SYLO030 시스템로그관리 MyBatis Mapper (로그 본문은 파일에서 읽고, 사용자 정보만 DB에서 조회)
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
public interface SystemLogMapper {

    /**
     * 사용자ID 목록에 해당하는 사용자 정보를 조회
     * @param usrIdList 사용자ID 목록 (1건 이상)
     * @return 사용자 정보 목록
     */
    List<LogUsrResponse> selectLogUsrList(@Param("usrIdList") Collection<String> usrIdList);

    /**
     * 사용자명(부분 일치)·사용자구분 조건에 맞는 사용자ID 목록을 조회
     * @param usrNm 사용자명 (없으면 조건 제외)
     * @param usrClsCd 사용자구분코드 (없으면 조건 제외)
     * @return 사용자ID 목록
     */
    List<String> selectLogUsrIdList(@Param("usrNm") String usrNm, @Param("usrClsCd") String usrClsCd);
}
