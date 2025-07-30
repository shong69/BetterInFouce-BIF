package com.sage.bif.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(servers())
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("BIF API")
                .description("BIF (Backend Integration Framework) REST API 문서")
                .version("1.0.0")
                .contact(new Contact()
                        .name("BIF Team")
                        .email("bif@example.com")
                        .url("https://github.com/SAGETeam"));
    }

    private List<Server> servers() {
        return List.of(
                new Server()
                        .url("http://localhost:8080/api")
                        .description("개발 서버"),
                new Server()
                        .url("https://your-production-domain.com/api")
                        .description("운영 서버")
        );
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }
} 