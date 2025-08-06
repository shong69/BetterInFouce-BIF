package com.sage.bif.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher; // 이 임포트가 필요합니다.

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authz -> authz
                // 1. Swagger UI 및 API Docs 관련 경로를 최상단에 허용 (가장 중요!)
                //    Context Path (/api)를 반드시 포함해야 합니다.
                .requestMatchers(
                    AntPathRequestMatcher.antMatcher("/api/swagger-ui/**"), // /api/swagger-ui/로 시작하는 모든 경로
                    AntPathRequestMatcher.antMatcher("/api/swagger-ui.html"), // /api/swagger-ui.html
                    AntPathRequestMatcher.antMatcher("/api/api-docs/**"),   // /api/api-docs/로 시작하는 모든 경로
                    AntPathRequestMatcher.antMatcher("/api/v3/api-docs/**"), // /api/v3/api-docs/로 시작하는 모든 경로
                    AntPathRequestMatcher.antMatcher("/api/v3/api-docs/swagger-config") // swagger-config도 명시적으로 허용
                ).permitAll() // 위의 경로들은 인증 없이 접근 허용

                // 2. H2 Console 접근 허용 (context-path가 붙지 않음을 다시 확인)
                //    application.yml에 h2.console.path: /h2-console 이므로 /api가 붙지 않습니다.
                .requestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")).permitAll()

                // 3. API 엔드포인트는 인증 필요
                //    /api로 시작하는 나머지 모든 요청은 인증 필요
                .requestMatchers("/api/**").authenticated()

                // 4. 그 외 모든 요청은 허용 (필요에 따라 .authenticated()로 변경 가능)
                .anyRequest().permitAll()
            )
            .httpBasic(httpBasic -> {}); // Basic 인증 활성화 (테스트용)

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}