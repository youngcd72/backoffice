package kr.or.coder.common.frame.service;

import kr.or.coder.common.frame.dto.auth.AuthMenuResponse;
import kr.or.coder.common.frame.mapper.AuthMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 로그인 사용자의 권한 메뉴 조회 Service 구현
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
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthMenuServiceImpl implements AuthMenuService {

    private final AuthMenuMapper authMenuMapper;

    /**
     * 사용자에게 권한이 있는 화면 메뉴와 그 상위 메뉴를 조회
     * @param usrId 사용자ID
     * @return 권한 메뉴 목록 (상위 메뉴 먼저, 정렬순번 순)
     */
    @Override
    public List<AuthMenuResponse> getAuthMenuList(String usrId) {
        return authMenuMapper.selectAuthMenuList(usrId);
    }
}
