package kr.or.coder.common.frame.mapper;

import kr.or.coder.common.frame.dto.favorite.FavoriteResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 사용자 즐겨찾기 메뉴(TB_PRGM_FAV_L) MyBatis Mapper
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
public interface FavoriteMapper {

    /**
     * 사용자의 즐겨찾기 메뉴 목록을 정렬순번 순으로 조회 (사용 중인 메뉴만)
     * @param usrId 사용자ID
     * @return 즐겨찾기 메뉴 목록
     */
    List<FavoriteResponse> selectFavoriteList(@Param("usrId") String usrId);
}
