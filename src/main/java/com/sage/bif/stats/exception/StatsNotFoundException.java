package com.sage.bif.stats.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class StatsNotFoundException extends BaseException {
    
    public StatsNotFoundException(Long statsId) {
        super(ErrorCode.STATS_NOT_FOUND, "Stats not found with id: " + statsId);
    }
    
    public StatsNotFoundException(Long userId, Long statsId) {
        super(ErrorCode.STATS_NOT_FOUND, "Stats not found with id: " + statsId + " for user: " + userId);
    }
    
    public StatsNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
} 