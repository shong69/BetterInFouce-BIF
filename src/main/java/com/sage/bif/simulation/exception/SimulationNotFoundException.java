package com.sage.bif.simulation.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class SimulationNotFoundException extends BaseException {
    
    public SimulationNotFoundException(Long simulationId) {
        super(ErrorCode.SIM_NOT_FOUND, "Simulation not found with id: " + simulationId);
    }

    public SimulationNotFoundException(Long userId, Long simulationId) {
        super(ErrorCode.SIM_NOT_FOUND, "Simulation not found with id: " + simulationId + " for user: " + userId);
    }
    
    public SimulationNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
} 