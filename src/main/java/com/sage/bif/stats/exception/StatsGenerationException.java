package com.sage.bif.stats.exception;

public class StatsGenerationException extends RuntimeException {
    public StatsGenerationException(String message) {
        super(message);
    }

    public StatsGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
