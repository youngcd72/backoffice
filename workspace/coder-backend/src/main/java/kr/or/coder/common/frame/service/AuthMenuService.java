package kr.or.coder.common.frame.service;

import kr.or.coder.common.frame.dto.auth.AuthMenuResponse;

import java.util.List;

/**
 * 로그인 사용자의 권한 메뉴 조회 Service (상단 메뉴·사이드바·메뉴 검색)
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
public interface AuthMenuService {

    /**
     * 사용자에게 권한이 있는 화면 메뉴와 그 상위 메뉴를 조회
     * @param usrId 사용자ID
     * @return 권한 메뉴 목록 (상위 메뉴 먼저, 정렬순번 순)
     */
    List<AuthMenuResponse> getAuthMenuList(String usrId);
}
