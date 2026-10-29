package kr.or.coder.api.common.log;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.MDC;

/**
 * 쿼리 실행 중 오류가 나면 해당 쿼리 ID({매퍼명}.{쿼리ID})를 MDC sqlId 에 남기는 MyBatis 인터셉터
 * (에러 로그의 SQL_ID 항목. MDC는 TraceIdFilter 가 요청 종료 시 비운다)
 * @author ywkim
 * @since 2026.10.27
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.10.27  ywkim        최초 생성
 * </pre>
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class SqlIdInterceptor implements Interceptor {

    /**
     * 쿼리를 실행하고, 예외가 나면 쿼리 ID를 MDC에 남긴 뒤 그대로 던진다
     * @param invocation MyBatis 실행 정보
     * @return 원래 실행 결과
     * @throws Throwable 쿼리 실행 중 발생한 예외
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        try {
            return invocation.proceed();
        } catch (Throwable e) {
            MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
            MDC.put(LogMdcKey.SQL_ID, getSqlId(mappedStatement.getId()));
            throw e;
        }
    }

    /**
     * 매핑 구문 전체 ID(패키지 포함)를 {매퍼명}.{쿼리ID} 형식으로 줄인다 (매퍼 XML SQL 주석과 같은 형식)
     * @param statementId 매핑 구문 전체 ID (kr.or.coder...SampleMapper.selectSample)
     * @return 매퍼명.쿼리ID (SampleMapper.selectSample)
     */
    private String getSqlId(String statementId) {
        int queryDot = statementId.lastIndexOf('.');
        int mapperDot = queryDot > 0 ? statementId.lastIndexOf('.', queryDot - 1) : -1;
        return statementId.substring(mapperDot + 1);
    }
}
