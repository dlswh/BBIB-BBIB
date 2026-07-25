package com.example.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.Scopes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "bearerAuth";
    private static final String SWAGGER_LOGIN = "swaggerLogin";

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        SecurityScheme swaggerLoginScheme = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .flows(new OAuthFlows()
                        .password(new OAuthFlow()
                                .tokenUrl("auth/swagger-token")
                                .scopes(new Scopes())));

        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, bearerScheme)
                        .addSecuritySchemes(SWAGGER_LOGIN, swaggerLoginScheme))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .addSecurityItem(new SecurityRequirement().addList(SWAGGER_LOGIN));
    }
}
