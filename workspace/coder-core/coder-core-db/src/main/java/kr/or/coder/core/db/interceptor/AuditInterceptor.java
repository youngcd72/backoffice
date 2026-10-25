package kr.or.coder.core.db.interceptor;

import kr.or.coder.core.db.model.BaseAuditDto;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Properties;

@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
/**
 * INSERT/UPDATE 시 BaseAuditDto의 생성일시·수정일시를 자동 설정하는 MyBatis 인터셉터
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
public class AuditInterceptor implements Interceptor {

    /**
     * 쿼리 실행 전에 BaseAuditDto 파라미터의 생성/수정 일시를 채운 뒤 실행을 진행
     * @param invocation MyBatis 실행 정보
     * @return 원래 실행 결과
     * @throws Throwable 실행 중 발생한 예외
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];

        if (parameter instanceof BaseAuditDto auditDto) {
            SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
            LocalDateTime now = LocalDateTime.now();

            if (SqlCommandType.INSERT.equals(sqlCommandType)) {
                if (auditDto.getCreatedAt() == null) {
                    auditDto.setCreatedAt(now);
                }
                if (auditDto.getUpdatedAt() == null) {
                    auditDto.setUpdatedAt(now);
                }
            } else if (SqlCommandType.UPDATE.equals(sqlCommandType)) {
                auditDto.setUpdatedAt(now);
            }
        }

        return invocation.proceed();
    }

    /**
     * 대상 객체를 이 인터셉터로 감싼 프록시를 반환
     * @param target 인터셉트 대상 객체
     * @return 인터셉터가 적용된 대상 객체
     */
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    /**
     * 인터셉터 속성을 설정 (현재 사용하지 않음)
     * @param properties 인터셉터 속성
     */
    @Override
    public void setProperties(Properties properties) {
        // Properties setup if needed
    }
}
