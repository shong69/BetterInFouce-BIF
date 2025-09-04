package com.sage.bif.common.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sage.bif.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler, AccessDeniedHandler, AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    
    public OAuth2AuthenticationFailureHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        handleSecurityError(response, HttpServletResponse.SC_UNAUTHORIZED, "OAUTH2_AUTHENTICATION_FAILED",
                "소셜 로그인 인증에 실패했습니다: " + exception.getMessage());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        handleSecurityError(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN",
                "접근 권한이 없습니다.");
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        handleSecurityError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED",
                "인증이 필요합니다.");
    }

    private void handleSecurityError(HttpServletResponse response, int statusCode,
                                     String errorCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiResponse<String> errorResponse = ApiResponse.error(errorCode, message);
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }

}
