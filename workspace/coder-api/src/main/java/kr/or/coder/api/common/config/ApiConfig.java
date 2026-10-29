package kr.or.coder.api.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 대외 연계 API 설정 프로퍼티(coder.api.*) 등록
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
@Configuration
@EnableConfigurationProperties(ApiProperties.class)
public class ApiConfig {
}
