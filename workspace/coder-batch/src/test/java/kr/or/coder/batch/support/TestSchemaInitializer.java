package kr.or.coder.batch.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

/**
 * 통합 테스트 컨텍스트마다 PostgreSQL 임시 스키마를 만들어 데이터소스를 연결하고, 컨텍스트 종료 시 삭제한다.
 * 개발 DB(coder 스키마)를 건드리지 않고 spring.sql.init 으로 테이블·테스트 데이터를 새로 만든다.
 * 접속 정보: coder.test.db.url / username / password (기본 로컬 도커 PostgreSQL)
 * @author ywkim
 * @since 2026.10.17
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.17  ywkim        최초 생성 (H2 대체)
 * </pre>
 */
public class TestSchemaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /** 임시 스키마 이름 접두사 (남은 스키마 식별용) */
    public static final String SCHEMA_PREFIX = "it_batch_";

    /**
     * 임시 스키마를 만들고 데이터소스 URL·계정을 그 스키마로 덮어쓴 뒤, 컨텍스트 종료 시 삭제하도록 등록
     * @param context 테스트 애플리케이션 컨텍스트
     * @throws IllegalStateException 테스트 DB에 접속하거나 스키마를 만들 수 없는 경우
     */
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment env = context.getEnvironment();
        String url = env.getProperty("coder.test.db.url", "jdbc:postgresql://localhost:5432/coder");
        String username = env.getProperty("coder.test.db.username", "coder");
        String password = env.getProperty("coder.test.db.password", "coder1234!");
        String schema = SCHEMA_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        execute(url, username, password, "CREATE SCHEMA " + schema);
        env.getPropertySources().addFirst(new MapPropertySource("coderTestSchema", Map.of(
                "spring.datasource.hikari.jdbc-url", url + "?currentSchema=" + schema,
                "spring.datasource.hikari.username", username,
                "spring.datasource.hikari.password", password)));
        context.addApplicationListener(event -> {
            if (event instanceof ContextClosedEvent closed && closed.getApplicationContext() == context) {
                execute(url, username, password, "DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        });
    }

    /**
     * 테스트 DB에 별도 커넥션으로 SQL 1건을 실행
     * @param url JDBC URL
     * @param username 계정
     * @param password 비밀번호
     * @param sql 실행할 SQL
     * @throws IllegalStateException 접속 또는 실행에 실패한 경우
     */
    private static void execute(String url, String username, String password, String sql) {
        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("테스트 DB 실행 실패 (" + url + "): " + sql, e);
        }
    }
}
