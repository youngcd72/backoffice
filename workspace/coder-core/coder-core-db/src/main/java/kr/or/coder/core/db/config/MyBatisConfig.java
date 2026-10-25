package kr.or.coder.core.db.config;

import kr.or.coder.core.db.interceptor.AuditInterceptor;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * MyBatis SqlSessionFactory, SqlSessionTemplate, 트랜잭션 매니저 설정
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
@EnableTransactionManagement
@RequiredArgsConstructor
@EnableConfigurationProperties(MyBatisProperties.class)
@MapperScan(basePackages = "kr.or.coder.**.mapper", sqlSessionFactoryRef = "sqlSessionFactory")
public class MyBatisConfig {

    private final MyBatisProperties myBatisProperties;
    private final AuditInterceptor auditInterceptor;

    /**
     * 매퍼 XML 위치, 타입 별칭, 감사 인터셉터를 적용한 SqlSessionFactory를 생성
     * @param dataSource 데이터소스
     * @return SqlSessionFactory
     * @throws Exception SqlSessionFactory 생성에 실패한 경우
     */
    @Bean
    @ConditionalOnMissingBean(SqlSessionFactory.class)
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactoryBean = new SqlSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource);
        sessionFactoryBean.setTypeAliasesPackage(myBatisProperties.getTypeAliasesPackage());

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sessionFactoryBean.setMapperLocations(resolver.getResources(myBatisProperties.getMapperLocations()));

        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(myBatisProperties.isMapUnderscoreToCamelCase());
        configuration.setCallSettersOnNulls(myBatisProperties.isCallSettersOnNulls());
        configuration.setUseGeneratedKeys(myBatisProperties.isUseGeneratedKeys());
        configuration.setDefaultStatementTimeout(myBatisProperties.getDefaultStatementTimeout());

        sessionFactoryBean.setConfiguration(configuration);
        sessionFactoryBean.setPlugins(auditInterceptor);

        return sessionFactoryBean.getObject();
    }

    /**
     * SqlSessionTemplate 빈을 생성
     * @param sqlSessionFactory SqlSessionFactory
     * @return SqlSessionTemplate
     */
    @Bean
    @ConditionalOnMissingBean(SqlSessionTemplate.class)
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    /**
     * DataSource 기반 트랜잭션 매니저를 생성
     * @param dataSource 데이터소스
     * @return 트랜잭션 매니저
     */
    @Bean
    @ConditionalOnMissingBean(PlatformTransactionManager.class)
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
