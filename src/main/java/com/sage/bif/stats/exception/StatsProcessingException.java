package com.sage.bif.stats.exception;

public class StatsProcessingException extends RuntimeException {
    
    public StatsProcessingException(String message) {
        super(message);
    }
    
    public StatsProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

}
