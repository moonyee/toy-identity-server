package com.platform.auth.identity.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Identity Server OpenAPI 3 설정.
 * <p>
 * Bearer JWT 보안 스키마를 전역 컴포넌트로 등록하고, 인증이 필요한 엔드포인트는
 * 각 컨트롤러 메서드에서 {@code @SecurityRequirement(name = "bearerAuth")} 로 선택적으로 참조한다.
 * <p>
 * 접근 경로 (context-path {@code /api} 적용):
 * <ul>
 *   <li>Swagger UI: {@code http://localhost:8182/api/swagger-ui.html}</li>
 *   <li>OpenAPI JSON: {@code http://localhost:8182/api/v3/api-docs}</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI identityServerOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Identity Server API")
                .description("회원가입 · 이메일 인증 · 로그인 · 로그아웃 · 탈퇴 등 사용자 인증 도메인 API")
                .version("v1.0"))
            .components(new Components()
                .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Bearer JWT 토큰. `/api/auth/login` 으로 발급받은 access token을 입력하세요.")));
    }
}
