package com.sage.bif.simulation.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import com.sage.bif.simulation.entity.Simulation;
import lombok.Getter;

@Getter
public class SimulationStartedEvent extends BaseEvent {
    
    private final Long simulationId;
    private final String sessionId;
    private final Long userId;
    private final String simulationTitle;
    private final String category;
    
    public SimulationStartedEvent(Object source, Long simulationId, String sessionId, 
                                Long userId, String simulationTitle, String category) {
        super(source);
        this.simulationId = simulationId;
        this.sessionId = sessionId;
        this.userId = userId;
        this.simulationTitle = simulationTitle;
        this.category = category;
    }
    
    public SimulationStartedEvent(Object source, Long simulationId, String sessionId, 
                                Long userId, String simulationTitle, String category, String correlationId) {
        super(source, correlationId);
        this.simulationId = simulationId;
        this.sessionId = sessionId;
        this.userId = userId;
        this.simulationTitle = simulationTitle;
        this.category = category;
    }
    
    @Override
    public String getEventType() {
        return "SIMULATION_STARTED";
    }
} 