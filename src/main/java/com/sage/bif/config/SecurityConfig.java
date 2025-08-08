package com.sage.bif.config;

import com.sage.bif.common.jwt.JwtAuthenticationFilter;
import com.sage.bif.common.jwt.JwtTokenProvider;
import com.sage.bif.common.oauth.OAuth2AuthenticationFailureHandler;
import com.sage.bif.common.oauth.OAuth2AuthenticationSuccessHandler;
import com.sage.bif.common.oauth.OAuth2UserServiceImpl;
import com.sage.bif.user.service.BifService;
import com.sage.bif.user.service.GuardianService;
import com.sage.bif.user.service.SocialLoginService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class SecurityConfig {

    @Value("${jwt.secret:your-secret-key-here-make-it-long-enough-for-hs256}")
    private String jwtSecret;

    @Bean
    public OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler(JwtTokenProvider jwtTokenProvider,
                                                                                 SocialLoginService socialLoginService,
                                                                                 BifService bifService,
                                                                                 GuardianService guardianService) {
        return new OAuth2AuthenticationSuccessHandler(jwtTokenProvider, socialLoginService, bifService, guardianService);
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
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorization -> authorization
                        .requestMatchers("/api/auth/admin-login", "/api/oauth2/**").permitAll()
                        .requestMatchers("/login/oauth2/code/**").permitAll()
                        .requestMatchers("/api/auth/register/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
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
}