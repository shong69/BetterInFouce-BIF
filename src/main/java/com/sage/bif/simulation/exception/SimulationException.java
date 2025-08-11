package com.sage.bif.simulation.exception;

import com.sage.bif.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class SimulationException extends RuntimeException {

    private final ErrorCode errorCode;

    public SimulationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public SimulationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SimulationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

}
