package com.sage.bif.simulation.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import lombok.Getter;

@Getter
public class SimulationCompletedEvent extends BaseEvent {
    
    private final String sessionId;
    private final Long simulationId;
    private final Integer finalScore;
    private final String finalGrade;
    private final Long userId;
    private final String simulationTitle;
    
    public SimulationCompletedEvent(Object source, String sessionId, Long simulationId, 
                                 Integer finalScore, String finalGrade, Long userId, String simulationTitle) {
        super(source);
        this.sessionId = sessionId;
        this.simulationId = simulationId;
        this.finalScore = finalScore;
        this.finalGrade = finalGrade;
        this.userId = userId;
        this.simulationTitle = simulationTitle;
    }
    
    public SimulationCompletedEvent(Object source, String sessionId, Long simulationId, 
                                 Integer finalScore, String finalGrade, Long userId, 
                                 String simulationTitle, String correlationId) {
        super(source, correlationId);
        this.sessionId = sessionId;
        this.simulationId = simulationId;
        this.finalScore = finalScore;
        this.finalGrade = finalGrade;
        this.userId = userId;
        this.simulationTitle = simulationTitle;
    }
    
    @Override
    public String getEventType() {
        return "SIMULATION_COMPLETED";
    }
} 