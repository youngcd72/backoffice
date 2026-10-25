package kr.or.coder.common.frame.service;

import kr.or.coder.common.frame.dto.favorite.FavoriteResponse;
import kr.or.coder.common.frame.mapper.FavoriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 사용자 즐겨찾기 메뉴 조회 Service 구현 (사이드바 즐겨찾기 탭)
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
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;

    /**
     * 사용자의 즐겨찾기 메뉴 목록을 조회
     * @param usrId 사용자ID
     * @return 즐겨찾기 메뉴 목록 (정렬순번 순)
     */
    @Override
    public List<FavoriteResponse> getFavoriteList(String usrId) {
        return favoriteMapper.selectFavoriteList(usrId);
    }
}
