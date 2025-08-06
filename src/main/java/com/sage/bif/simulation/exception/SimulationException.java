package com.sage.bif.simulation.exception;

import lombok.Getter;

@Getter
public class SimulationException extends RuntimeException {
    private final SimulationErrorCode errorCode;

    public SimulationException(SimulationErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public SimulationException(SimulationErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SimulationException(SimulationErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
} 