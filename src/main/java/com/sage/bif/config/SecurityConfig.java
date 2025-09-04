package com.sage.bif.config;

import com.sage.bif.common.jwt.JwtAuthenticationFilter;
import com.sage.bif.common.jwt.JwtTokenProvider;
import com.sage.bif.common.oauth.OAuth2AuthenticationFailureHandler;
import com.sage.bif.common.oauth.OAuth2AuthenticationSuccessHandler;
import com.sage.bif.common.oauth.OAuth2UserServiceImpl;
import com.sage.bif.user.service.BifService;
import com.sage.bif.user.service.GuardianService;
import com.sage.bif.user.service.LoginLogService;
import com.sage.bif.user.service.SocialLoginService;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler(JwtTokenProvider jwtTokenProvider,
                                                                                 SocialLoginService socialLoginService,
                                                                                 BifService bifService,
                                                                                 GuardianService guardianService,
                                                                                 LoginLogService loginLogService) {
        return new OAuth2AuthenticationSuccessHandler(jwtTokenProvider, socialLoginService, bifService, guardianService, loginLogService);
    }

    @Bean
    public OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler() {
        return new OAuth2AuthenticationFailureHandler();
    }

    @Bean
    public OAuth2UserServiceImpl oAuth2UserServiceImpl() {
        return new OAuth2UserServiceImpl();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, OAuth2UserServiceImpl oAuth2UserServiceImpl,
                                                   OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                                                   OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorization -> authorization
                        .requestMatchers("/api/auth/admin-login", "/api/oauth2/**").permitAll()
                        .requestMatchers("/login/oauth2/code/**").permitAll()
                        .requestMatchers("/api/auth/register/**", "/api/auth/session-info", "/api/auth/logout", "/api/auth/refresh").permitAll()
                        .requestMatchers("/api/notifications/sse/subscribe").authenticated()
                        .requestMatchers("/api/notifications/web-push/**").authenticated()
                        .requestMatchers("/api/notifications/status").authenticated()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserServiceImpl)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedHandler(oAuth2AuthenticationFailureHandler)
                        .authenticationEntryPoint(oAuth2AuthenticationFailureHandler)
                )
                .httpBasic(httpBasic -> {});

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(frontendUrl));
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
