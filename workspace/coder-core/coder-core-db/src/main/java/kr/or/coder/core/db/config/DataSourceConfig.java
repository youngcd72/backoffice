package kr.or.coder.core.db.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * HikariCP 데이터소스 설정
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
@Configuration
public class DataSourceConfig {

    /**
     * spring.datasource.hikari 설정을 바인딩한 HikariConfig 빈을 생성
     * @return Hikari 커넥션 풀 설정
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariConfig hikariConfig() {
        return new HikariConfig();
    }

    /**
     * HikariCP 기반 DataSource 빈을 생성 (다른 DataSource가 없을 때만)
     * @param hikariConfig Hikari 커넥션 풀 설정
     * @return HikariDataSource
     */
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource(HikariConfig hikariConfig) {
        return new HikariDataSource(hikariConfig);
    }
}
