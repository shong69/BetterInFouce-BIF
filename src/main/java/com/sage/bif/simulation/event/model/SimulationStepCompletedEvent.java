package com.sage.bif.simulation.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import com.sage.bif.simulation.entity.Simulation;
import lombok.Getter;

@Getter
public class SimulationStepCompletedEvent extends BaseEvent {
    
    private final Simulation simulation;
    private final Long userId;
    private final String stepName;
    private final Integer stepNumber;
    private final String stepResult;
    
    public SimulationStepCompletedEvent(Object source, Simulation simulation, Long userId, 
                                      String stepName, Integer stepNumber, String stepResult) {
        super(source);
        this.simulation = simulation;
        this.userId = userId;
        this.stepName = stepName;
        this.stepNumber = stepNumber;
        this.stepResult = stepResult;
    }
    
    public SimulationStepCompletedEvent(Object source, Simulation simulation, Long userId, 
                                      String stepName, Integer stepNumber, String stepResult, String correlationId) {
        super(source, correlationId);
        this.simulation = simulation;
        this.userId = userId;
        this.stepName = stepName;
        this.stepNumber = stepNumber;
        this.stepResult = stepResult;
    }
    
    @Override
    public String getEventType() {
        return "SIMULATION_STEP_COMPLETED";
    }
} 