package kr.or.coder.core.security.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(OpenAPI) 문서 설정 (Bearer JWT 및 OAuth2 인증 스킴)
 * @author ywkim
 * @since 2026.08.06
 * @version 1.0
 *
 * <pre>
 * 수정일       수정자               수정내용
 * ----------  --------    ---------------------------
 * 2026.08.06  ywkim        최초 생성
 * </pre>
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(SwaggerProperties.class)
@ConditionalOnProperty(name = "coder.swagger.enabled", havingValue = "true", matchIfMissing = true)
public class SwaggerConfig {

    private final SwaggerProperties swaggerProperties;

    /**
     * API 정보와 보안 스킴(Bearer, OAuth2)을 포함한 OpenAPI 빈을 생성
     * @return OpenAPI
     */
    @Bean
    public OpenAPI openAPI() {
        String bearerSchemeName = "BearerAuth";
        String oauth2SchemeName = "OAuth2Auth";

        Components components = new Components()
                .addSecuritySchemes(bearerSchemeName, new SecurityScheme()
                        .name(bearerSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT Bearer Token Authentication"));

        if (swaggerProperties.getOauth2().isEnabled()) {
            OAuthFlows flows = new OAuthFlows()
                    .authorizationCode(new OAuthFlow()
                            .authorizationUrl(swaggerProperties.getOauth2().getAuthorizationUrl())
                            .tokenUrl(swaggerProperties.getOauth2().getTokenUrl())
                            .scopes(new Scopes().addString(swaggerProperties.getOauth2().getScope(), "Default scope")));

            components.addSecuritySchemes(oauth2SchemeName, new SecurityScheme()
                    .type(SecurityScheme.Type.OAUTH2)
                    .description("OAuth2 Authorization Code Flow")
                    .flows(flows));
        }

        return new OpenAPI()
                .info(new Info()
                        .title(swaggerProperties.getTitle())
                        .description(swaggerProperties.getDescription())
                        .version(swaggerProperties.getVersion()))
                .components(components)
                .addSecurityItem(new SecurityRequirement().addList(bearerSchemeName))
                .addSecurityItem(new SecurityRequirement().addList(oauth2SchemeName));
    }
}
