package kr.or.coder.common.log;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.annotation.Configuration;

/**
 * SqlIdInterceptor 를 SqlSessionFactory 에 등록하는 설정
 * (coder-core-db MyBatisConfig 는 AuditInterceptor 만 등록하므로 생성 후 추가한다)
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
@RequiredArgsConstructor
public class SqlIdInterceptorConfig {

    private final SqlSessionFactory sqlSessionFactory;

    /**
     * SqlSessionFactory 설정에 SqlIdInterceptor 를 추가
     */
    @PostConstruct
    public void addSqlIdInterceptor() {
        sqlSessionFactory.getConfiguration().addInterceptor(new SqlIdInterceptor());
    }
}
