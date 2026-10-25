package kr.or.coder.core.kms.local;

import kr.or.coder.core.kms.SecretResolver;
import org.springframework.core.env.Environment;

import java.util.Map;

/**
 * 로컬 비밀 저장소: coder.kms.local.secrets 맵 → 환경변수 순으로 조회한다.
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
public class LocalSecretResolver implements SecretResolver {

    private final Map<String, String> secrets;
    private final Environment environment;

    /**
     * 비밀 맵과 환경을 주입받아 생성
     * @param secrets 참조키 → 비밀값 맵
     * @param environment 스프링 환경 (환경변수 조회용)
     */
    public LocalSecretResolver(Map<String, String> secrets, Environment environment) {
        this.secrets = secrets;
        this.environment = environment;
    }

    /**
     * 참조키로 비밀값을 조회 (맵 우선, 없으면 환경 프로퍼티)
     * @param reference 비밀 참조키
     * @return 비밀값
     * @throws IllegalStateException 참조키에 해당하는 비밀값이 없을 때
     */
    @Override
    public String resolve(String reference) {
        String value = secrets.get(reference);
        if (value == null) {
            value = environment.getProperty(reference);
        }
        if (value == null) {
            throw new IllegalStateException("secret not found for reference: " + reference);
        }
        return value;
    }
}
