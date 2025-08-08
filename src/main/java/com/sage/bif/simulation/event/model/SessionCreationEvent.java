package com.sage.bif.simulation.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import lombok.Getter;

@Getter
public class SessionCreationEvent extends BaseEvent {
    
    private final Long simulationId;
    private String sessionId; // 생성된 세션 ID를 담을 필드
    
    public SessionCreationEvent(Object source, Long simulationId) {
        super(source);
        this.simulationId = simulationId;
    }
    
    public SessionCreationEvent(Object source, Long simulationId, String correlationId) {
        super(source, correlationId);
        this.simulationId = simulationId;
    }
    
    // 생성된 세션 ID 설정 메서드
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    @Override
    public String getEventType() {
        return "SESSION_CREATION";
    }
} 