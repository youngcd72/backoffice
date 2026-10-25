package kr.or.coder.common.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 로그인·권한 설정 (비밀번호 암호화, 화면 권한 확인 Interceptor 등록)
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
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfig implements WebMvcConfigurer {

    private final PrgmAuthInterceptor prgmAuthInterceptor;

    /**
     * 비밀번호 BCrypt 암호화 인코더를 생성
     * @return PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * API 요청에 화면 권한 확인 Interceptor 를 등록
     * @param registry Interceptor 등록 객체
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(prgmAuthInterceptor).addPathPatterns("/api/**");
    }
}
