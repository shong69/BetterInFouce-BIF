package com.sage.bif.stats.exception;

/**
 * 통계 데이터 파싱 중 발생하는 예외
 */
public class StatsDataParseException extends RuntimeException {
    
    public StatsDataParseException(String message) {
        super(message);
    }
    
    public StatsDataParseException(String message, Throwable cause) {
        super(message, cause);
    }
} 