package kr.or.coder.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CODER Backend Spring Boot 애플리케이션 진입점
 * @author ywkim
 * @since 2026.09.30
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.09.30  ywkim        최초 생성
 * </pre>
 */
@SpringBootApplication(scanBasePackages = {"kr.or.coder.biz", "kr.or.coder.common", "kr.or.coder.core"})
@MapperScan("kr.or.coder.common.frame.mapper")
public class CoderBackendApplication {

    /**
     * 애플리케이션을 기동
     * @param args 실행 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(CoderBackendApplication.class, args);
    }
}
