package kr.or.coder.core.kms.config;

import kr.or.coder.core.kms.KmsClient;
import kr.or.coder.core.kms.SecretResolver;
import kr.or.coder.core.kms.local.LocalKmsClient;
import kr.or.coder.core.kms.local.LocalSecretResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.time.Clock;

/**
 * KMS 클라이언트·비밀 저장소·Clock 빈 설정 (provider 설정에 따라 구현체 선택)
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
@Configuration
@EnableConfigurationProperties(KmsProperties.class)
public class KmsConfig {

    private static final String LOCAL = "local";

    /**
     * 시스템 기본 시간대의 Clock 빈을 생성
     * @return 시스템 기본 Clock
     */
    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /**
     * coder.kms.provider 설정에 맞는 KmsClient 구현체를 생성
     * @param properties KMS 설정
     * @param environment 스프링 환경 (활성 프로파일 확인용)
     * @param clock 시간 기준 Clock
     * @return KmsClient 구현체
     * @throws IllegalStateException local 외 provider이거나 prod 프로파일에서 local을 사용하는 경우
     */
    @Bean
    @ConditionalOnMissingBean(KmsClient.class)
    public KmsClient kmsClient(KmsProperties properties, Environment environment, Clock clock) {
        if (LOCAL.equals(properties.getProvider())) {
            rejectInProduction(environment);
            return new LocalKmsClient(properties.getLocal().getMasterKey(), clock);
        }
        throw new IllegalStateException("KMS provider '" + properties.getProvider()
                + "' is not implemented yet. Decide the KMS product and add a KmsClient implementation.");
    }

    /**
     * coder.kms.provider 설정에 맞는 SecretResolver 구현체를 생성
     * @param properties KMS 설정
     * @param environment 스프링 환경 (활성 프로파일 확인 및 환경변수 조회용)
     * @return SecretResolver 구현체
     * @throws IllegalStateException local 외 provider이거나 prod 프로파일에서 local을 사용하는 경우
     */
    @Bean
    @ConditionalOnMissingBean(SecretResolver.class)
    public SecretResolver secretResolver(KmsProperties properties, Environment environment) {
        if (LOCAL.equals(properties.getProvider())) {
            rejectInProduction(environment);
            return new LocalSecretResolver(properties.getLocal().getSecrets(), environment);
        }
        throw new IllegalStateException("SecretResolver for provider '" + properties.getProvider()
                + "' is not implemented yet.");
    }

    /**
     * prod 프로파일에서 local KMS 사용을 차단
     * @param environment 스프링 환경
     * @throws IllegalStateException prod 프로파일이 활성화된 경우
     */
    private void rejectInProduction(Environment environment) {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("coder.kms.provider=local is not allowed in the prod profile.");
        }
    }
}
