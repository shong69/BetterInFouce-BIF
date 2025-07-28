package com.sage.bif;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/hello")
public class HelloController {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${app.environment:unknown}")
    private String environment;

    @GetMapping
    public String hello() {
        log.info("Hello endpoint 호출됨 - 애플리케이션: {}", applicationName);
        String response = "Hello, " + applicationName + "! 🚀";
        log.debug("Hello 응답: {}", response);
        return response;
    }

    @GetMapping("/health")
    public String health() {
        log.info("Health check endpoint 호출됨");
        String response = applicationName + " Application is running! ✅";
        log.debug("Health check 응답: {}", response);
        return response;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        log.info("Info endpoint 호출됨 - 환경: {}", environment);
        Map<String, Object> info = new HashMap<>();
        info.put("application", applicationName);
        info.put("environment", environment);
        info.put("timestamp", LocalDateTime.now());
        info.put("status", "running");
        log.debug("Info 응답 데이터: {}", info);
        return info;
    }

    @GetMapping("/test")
    public Map<String, String> test() {
        log.info("Test endpoint 호출됨");
        Map<String, String> response = new HashMap<>();
        response.put("message", "BIF API is working correctly!");
        response.put("environment", environment);
        response.put("application", applicationName);
        log.debug("Test 응답: {}", response);
        return response;
    }
} 