package com.sage.bif.simulation.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import lombok.Getter;

@Getter
public class ResultCalculationEvent extends BaseEvent {
    
    private final String sessionId;
    private final Long simulationId;
    private final Integer totalScore;
    private final Integer percentage;
    private final String finalGrade;
    private final String feedbackMessage;
    
    public ResultCalculationEvent(Object source, String sessionId, Long simulationId, 
                               Integer totalScore, Integer percentage, String finalGrade, String feedbackMessage) {
        super(source);
        this.sessionId = sessionId;
        this.simulationId = simulationId;
        this.totalScore = totalScore;
        this.percentage = percentage;
        this.finalGrade = finalGrade;
        this.feedbackMessage = feedbackMessage;
    }
    
    public ResultCalculationEvent(Object source, String sessionId, Long simulationId, 
                               Integer totalScore, Integer percentage, String finalGrade, 
                               String feedbackMessage, String correlationId) {
        super(source, correlationId);
        this.sessionId = sessionId;
        this.simulationId = simulationId;
        this.totalScore = totalScore;
        this.percentage = percentage;
        this.finalGrade = finalGrade;
        this.feedbackMessage = feedbackMessage;
    }
    
    @Override
    public String getEventType() {
        return "RESULT_CALCULATION";
    }
} 