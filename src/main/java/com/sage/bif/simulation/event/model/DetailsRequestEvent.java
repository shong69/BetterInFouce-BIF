package com.sage.bif.simulation.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import lombok.Getter;

@Getter
public class DetailsRequestEvent extends BaseEvent {
    
    private final Long simulationId;
    private final String requestId;
    
    public DetailsRequestEvent(Object source, Long simulationId, String requestId) {
        super(source);
        this.simulationId = simulationId;
        this.requestId = requestId;
    }
    
    public DetailsRequestEvent(Object source, Long simulationId, String requestId, String correlationId) {
        super(source, correlationId);
        this.simulationId = simulationId;
        this.requestId = requestId;
    }
    
    @Override
    public String getEventType() {
        return "DETAILS_REQUEST";
    }
} 