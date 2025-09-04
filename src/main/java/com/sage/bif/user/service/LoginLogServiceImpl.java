package com.sage.bif.user.service;

import com.sage.bif.user.entity.UserLoginLogs;
import com.sage.bif.user.repository.UserLoginLogsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoginLogServiceImpl implements LoginLogService {

    private final UserLoginLogsRepository userLoginLogsRepository;

    @Override
    public UserLoginLogs recordLogin(Long socialId) {

        UserLoginLogs loginLog = UserLoginLogs.builder()
                .socialId(socialId)
                .loginAt(LocalDateTime.now())
                .build();

        return userLoginLogsRepository.save(loginLog);
    }

    @Override
    public void recordLogout(Long socialId) {

        userLoginLogsRepository.findLatestActiveSession(socialId)
                .ifPresentOrElse(
                        activeSession -> {
                            activeSession.setLogoutAt(LocalDateTime.now());
                            userLoginLogsRepository.save(activeSession);
                        },
                        () -> log.warn("활성 세션이 없습니다: socialId={}", socialId)
                );
    }

    @Override
    public int scheduleLogsForDeletion(Long socialId, LocalDateTime withdrawalDate) {
        LocalDateTime deletableAfter = withdrawalDate.plusMonths(3);

        return userLoginLogsRepository.scheduleUserLogsDeletion(socialId, deletableAfter);
    }

    @Override
    public int cleanupExpiredLogs() {
        LocalDateTime now = LocalDateTime.now();

        long expiredCount = userLoginLogsRepository.countExpiredLogs(now);

        if (expiredCount == 0) {
            return 0;
        }

        return userLoginLogsRepository.deleteExpiredLogs(now);
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void scheduledCleanup() {
        log.info("=== 자동 로그 정리 시작 ===");

        try {
            int deletedCount = cleanupExpiredLogs();

            if (deletedCount > 0) {
                log.info("✅ 만료된 로그 {}건 자동 삭제 완료", deletedCount);
            }

        } catch (Exception e) {
            log.error("❌ 자동 로그 정리 중 오류 발생", e);
        }

        log.info("=== 자동 로그 정리 완료 ===");
    }

}
