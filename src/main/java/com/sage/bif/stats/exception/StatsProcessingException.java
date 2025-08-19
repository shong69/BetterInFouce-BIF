package com.sage.bif.stats.exception;

public class StatsProcessingException extends RuntimeException {
    public StatsProcessingException(final String message) {
        super(message);
    }

    public StatsProcessingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
