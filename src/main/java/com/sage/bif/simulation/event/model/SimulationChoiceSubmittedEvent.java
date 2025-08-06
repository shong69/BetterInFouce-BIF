package com.sage.bif.simulation.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import lombok.Getter;

@Getter
public class SimulationChoiceSubmittedEvent extends BaseEvent {
    
    private final String sessionId;
    private final Long simulationId;
    private final String selectedChoice;
    private final Integer currentStep;
    private final Integer currentScore;
    private final Long userId;
    
    public SimulationChoiceSubmittedEvent(Object source, String sessionId, Long simulationId, 
                                       String selectedChoice, Integer currentStep, 
                                       Integer currentScore, Long userId) {
        super(source);
        this.sessionId = sessionId;
        this.simulationId = simulationId;
        this.selectedChoice = selectedChoice;
        this.currentStep = currentStep;
        this.currentScore = currentScore;
        this.userId = userId;
    }
    
    public SimulationChoiceSubmittedEvent(Object source, String sessionId, Long simulationId, 
                                       String selectedChoice, Integer currentStep, 
                                       Integer currentScore, Long userId, String correlationId) {
        super(source, correlationId);
        this.sessionId = sessionId;
        this.simulationId = simulationId;
        this.selectedChoice = selectedChoice;
        this.currentStep = currentStep;
        this.currentScore = currentScore;
        this.userId = userId;
    }
    
    @Override
    public String getEventType() {
        return "SIMULATION_CHOICE_SUBMITTED";
    }
} 