package com.sage.bif.stats.exception;

public class StatsParsingException extends RuntimeException {
    public StatsParsingException(String message) {
        super(message);
    }

    public StatsParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
