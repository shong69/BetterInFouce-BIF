package com.sage.bif.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Redis SET - Key: {}", key);
        } catch (Exception e) {
            log.error("Redis SET 실패 - Key: {}, Error: {}", key, e.getMessage());
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Redis SET with TTL - Key: {}, TTL: {}", key, ttl);
        } catch (Exception e) {
            log.error("Redis SET with TTL 실패 - Key: {}, Error: {}", key, e.getMessage());
        }
    }

    public Optional<Object> get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            log.debug("Redis GET - Key: {}, Value: {}", key, value);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            log.error("Redis GET 실패 - Key: {}, Error: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            log.debug("Redis DELETE - Key: {}, Result: {}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Redis DELETE 실패 - Key: {}, Error: {}", key, e.getMessage());
            return false;
        }
    }

}
