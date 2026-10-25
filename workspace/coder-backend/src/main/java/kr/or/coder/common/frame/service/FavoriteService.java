package kr.or.coder.common.frame.service;

import kr.or.coder.common.frame.dto.favorite.FavoriteResponse;

import java.util.List;

/**
 * 사용자 즐겨찾기 메뉴 조회 Service (사이드바 즐겨찾기 탭)
 * @author ywkim
 * @since 2026.10.20
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.20  ywkim        최초 생성 (구체 클래스 FavoriteService를 인터페이스와 구현으로 분리)
 * </pre>
 */
public interface FavoriteService {

    /**
     * 사용자의 즐겨찾기 메뉴 목록을 조회
     * @param usrId 사용자ID
     * @return 즐겨찾기 메뉴 목록 (정렬순번 순)
     */
    List<FavoriteResponse> getFavoriteList(String usrId);
}
