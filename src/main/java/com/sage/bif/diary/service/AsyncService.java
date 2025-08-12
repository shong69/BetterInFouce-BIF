package com.sage.bif.diary.service;

import com.sage.bif.common.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncService {

    private final RedisService redisService;

    @Async("cacheTaskExecutor")
    public void invalidateCacheAsync(Long bifId, LocalDate date) {
        try {
            String cacheKey = String.format("monthly_summary:%d:%d:%d",
                    bifId, date.getYear(), date.getMonthValue());

            redisService.delete(cacheKey);
            log.info("캐시 무효화 완료 : {}", cacheKey);
        } catch (Exception e) {
            log.warn("캐시 무효화 실패: {}", e.getMessage());
        }
    }

}
