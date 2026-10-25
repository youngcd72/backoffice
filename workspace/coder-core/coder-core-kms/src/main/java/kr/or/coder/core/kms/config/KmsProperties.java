package kr.or.coder.core.kms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * KMS 설정(coder.kms.*) 프로퍼티
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
@Getter
@Setter
@ConfigurationProperties(prefix = "coder.kms")
public class KmsProperties {

    /**
     * KMS 구현체: local(로컬/테스트 전용), vault, aws ... (제품 결정 전까지 local만 구현)
     */
    private String provider = "local";

    private Local local = new Local();

    /**
     * 로컬 KMS 설정 (KEK, 비밀 저장소)
     */
    @Getter
    @Setter
    public static class Local {

        /**
         * 로컬 KEK (Base64, 32 bytes). 비우면 기동마다 새로 생성한다 (재기동 시 기존 암호문 복호화 불가).
         */
        private String masterKey;

        /**
         * 로컬 비밀 저장소: 참조키 → 비밀값. 없으면 같은 이름의 환경변수를 찾는다.
         */
        private Map<String, String> secrets = new LinkedHashMap<>();
    }
}
