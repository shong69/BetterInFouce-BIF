package com.sage.bif.stats.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class StatsNotFoundException extends BaseException {
    

    public StatsNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
} 