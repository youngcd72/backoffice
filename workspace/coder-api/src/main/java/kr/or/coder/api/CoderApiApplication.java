package kr.or.coder.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CODER 대외 연계 API(coder-api) Spring Boot 애플리케이션 진입점
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
@SpringBootApplication(scanBasePackages = {"kr.or.coder.api", "kr.or.coder.core"})
public class CoderApiApplication {

    /**
     * 애플리케이션을 기동
     * @param args 실행 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(CoderApiApplication.class, args);
    }
}
