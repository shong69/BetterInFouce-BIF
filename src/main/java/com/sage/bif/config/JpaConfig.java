package com.sage.bif.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA 설정 클래스
 * JPA 관련 추가 설정들을 관리
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.sage.bif")
public class JpaConfig {
    

} 