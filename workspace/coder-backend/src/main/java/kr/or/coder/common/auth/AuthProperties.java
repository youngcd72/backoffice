package kr.or.coder.common.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 로그인·권한 설정 (application.yml coder.auth)
 * @author ywkim
 * @since 2026.10.20
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.20  ywkim        최초 생성
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "coder.auth")
public class AuthProperties {

    /** 사용자 등록·비밀번호 초기화 시 설정하는 초기 비밀번호 */
    private String initPassword;

    /** 비밀번호를 연속으로 틀리면 계정을 잠그는 횟수 */
    private int maxLoginFailCount = 5;

    /** 관리자(role ADMIN)로 보는 권한그룹 코드 목록 (적용기간 내 소속 사용자) */
    private List<String> adminGroupCodes = new ArrayList<>();
}
