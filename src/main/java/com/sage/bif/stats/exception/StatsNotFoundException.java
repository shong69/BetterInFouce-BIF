package com.sage.bif.stats.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class StatsNotFoundException extends BaseException {
    
    public StatsNotFoundException(final Long statsId) {
        super(ErrorCode.STATS_NOT_FOUND, "Stats not found with id: " + statsId);
    }

    public StatsNotFoundException(final Long userId, final Long statsId) {
        super(ErrorCode.STATS_NOT_FOUND, "Stats not found with id: " + statsId + " for user: " + userId);
    }

    public StatsNotFoundException(final ErrorCode errorCode, final String message) {
        super(errorCode, message);
    }
}
