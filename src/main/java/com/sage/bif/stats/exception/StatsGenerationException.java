package com.sage.bif.stats.exception;

public class StatsGenerationException extends RuntimeException {

    public StatsGenerationException(final String message) {
        super(message);
    }

    public StatsGenerationException(final String message, final Throwable cause) {
        super(message, cause);
    }

}

