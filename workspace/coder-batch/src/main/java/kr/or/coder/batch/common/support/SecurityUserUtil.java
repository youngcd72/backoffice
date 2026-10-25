package kr.or.coder.batch.common.support;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 현재 인증 사용자 정보 조회 유틸리티
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
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityUserUtil {

    /**
     * 현재 요청 사용자 ID(JWT subject)를 사용자ID 컬럼 길이에 맞춰 반환한다. 인증 정보가 없으면 defaultUserId.
     * @param defaultUserId 인증 정보가 없을 때 사용할 기본 사용자 ID
     * @return 현재 사용자 ID 또는 기본 사용자 ID
     */
    public static String getCurrentUserId(String defaultUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null) {
            return defaultUserId;
        }
        return BatchTextUtil.limitUserId(authentication.getName());
    }
}
