package kr.or.coder.core.db.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MyBatis 설정(coder.mybatis.*) 프로퍼티
 * @author ywkim
 * @since 2026.08.04
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.08.04  ywkim        최초 생성
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "coder.mybatis")
public class MyBatisProperties {

    private String mapperLocations = "classpath*:mapper/**/*.xml";
    private String typeAliasesPackage = "kr.or.coder";
    private boolean mapUnderscoreToCamelCase = true;
    private boolean callSettersOnNulls = true;
    private boolean useGeneratedKeys = true;
    private int defaultStatementTimeout = 30;
}
