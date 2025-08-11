package com.sage.bif.user.service;

import java.time.LocalDateTime;

import com.sage.bif.user.entity.UserLoginLogs;

public interface LoginLogService {

    UserLoginLogs recordLogin(Long socialId);
    void recordLogout(Long socialId);
    int scheduleLogsForDeletion(Long socialId, LocalDateTime withdrawalDate);
    int cleanupExpiredLogs();

}
