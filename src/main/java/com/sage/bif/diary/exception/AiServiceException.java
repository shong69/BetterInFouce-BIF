package com.sage.bif.diary.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
import lombok.Getter;

import java.util.Map;

@Getter
public class AiServiceException extends BaseException {
    
    private final Map<String, Object> violationInfo;
    
    public AiServiceException(ErrorCode errorCode) {
        super(errorCode);
        this.violationInfo = null;
    }
    
    public AiServiceException(ErrorCode errorCode, Object details) {
        super(errorCode, details);
        this.violationInfo = details instanceof Map ? (Map<String, Object>) details : null;
    }
    
    public AiServiceException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
        this.violationInfo = null;
    }
    
    public AiServiceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
        this.violationInfo = null;
    }
    
    public AiServiceException(ErrorCode errorCode, String message, Map<String, Object> violationInfo) {
        super(errorCode, message);
        this.violationInfo = violationInfo;
    }
} 