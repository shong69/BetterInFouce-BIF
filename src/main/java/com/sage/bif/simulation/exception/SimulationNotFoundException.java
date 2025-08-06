package com.sage.bif.simulation.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class SimulationNotFoundException extends BaseException {
    

    
    public SimulationNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
} 