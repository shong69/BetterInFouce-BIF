package com.sage.bif.stats.exception;

public class StatsDataParseException extends RuntimeException {
    
    public StatsDataParseException(final String message) {
        super(message);
    }

    public StatsDataParseException(final String message, final Throwable cause) {
        super(message, cause);
    }

} 
