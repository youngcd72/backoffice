package kr.or.coder.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CODER 배치 애플리케이션 진입점.
 * <p>
 * {@code @EnableBatchProcessing}은 선언하지 않는다 (Boot 3의 BatchAutoConfiguration이 비활성화됨).
 * Mapper 스캔은 coder-core-db의 MyBatisConfig({@code kr.or.coder.**.mapper})가 처리한다.
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
@SpringBootApplication(scanBasePackages = {"kr.or.coder.batch", "kr.or.coder.core"})
public class CoderBatchApplication {

    /**
     * 배치 애플리케이션을 기동한다.
     * @param args 실행 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(CoderBatchApplication.class, args);
    }
}
