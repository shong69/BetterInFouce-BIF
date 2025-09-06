package com.sage.bif.stats.exception;

public class StatsParsingException extends RuntimeException {

    public StatsParsingException(final String message) {
        super(message);
    }

    public StatsParsingException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
