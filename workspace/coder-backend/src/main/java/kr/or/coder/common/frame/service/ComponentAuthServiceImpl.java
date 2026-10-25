package kr.or.coder.common.frame.service;

import kr.or.coder.common.frame.dto.auth.ComponentAuthResponse;
import kr.or.coder.common.frame.mapper.ComponentAuthMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 로그인 사용자의 화면 컴포넌트(버튼 등) 권한 조회 Service 구현
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
public class ComponentAuthServiceImpl implements ComponentAuthService {

    private final ComponentAuthMapper componentAuthMapper;

    /**
     * 프로그램의 컴포넌트별 사용자 권한여부를 조회
     * @param usrId 사용자ID
     * @param prgmId 프로그램ID
     * @return 컴포넌트 권한 목록 (TB_PRGM_CMPN_M 에 사용 중으로 등록된 컴포넌트만)
     */
    @Override
    public List<ComponentAuthResponse> getComponentAuthList(String usrId, String prgmId) {
        return componentAuthMapper.selectComponentAuthList(usrId, prgmId);
    }
}
