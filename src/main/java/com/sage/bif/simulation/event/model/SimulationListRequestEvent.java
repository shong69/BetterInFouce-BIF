package com.sage.bif.simulation.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import com.sage.bif.simulation.dto.response.SimulationResponse;
import lombok.Getter;
import java.util.List;

@Getter
public class SimulationListRequestEvent extends BaseEvent {
    
    private final String requestId;
    private List<SimulationResponse> result; // 결과를 담을 필드 추가
    
    public SimulationListRequestEvent(Object source, String requestId) {
        super(source);
        this.requestId = requestId;
    }
    
    public SimulationListRequestEvent(Object source, String requestId, String correlationId) {
        super(source, correlationId);
        this.requestId = requestId;
    }
    
    // 결과 설정 메서드 추가
    public void setResult(List<SimulationResponse> result) {
        this.result = result;
    }
    
    @Override
    public String getEventType() {
        return "SIMULATION_LIST_REQUEST";
    }
} 