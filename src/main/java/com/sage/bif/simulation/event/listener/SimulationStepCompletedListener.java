package com.sage.bif.simulation.event.listener;

import com.sage.bif.simulation.event.model.SimulationStepCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SimulationStepCompletedListener {
    
    @EventListener
    public void handleSimulationStepCompleted(SimulationStepCompletedEvent event) {
        log.info("Simulation step completed: {} - User: {} - Step: {} - Result: {} - EventId: {}", 
                event.getSimulation().getId(), event.getUserId(), event.getStepName(), 
                event.getStepResult(), event.getEventId());
        
        // 여기에 시뮬레이션 단계 완료 처리 로직 추가
        // 예: 다음 단계 준비, 진행률 업데이트 등
    }
    
    @Async
    @EventListener
    public void handleSimulationStepCompletedAsync(SimulationStepCompletedEvent event) {
        log.info("Async processing simulation step completion: {} - User: {} - Step: {} - EventId: {}", 
                event.getSimulation().getId(), event.getUserId(), event.getStepName(), event.getEventId());
        
        // 비동기 처리 로직
        // 예: 통계 업데이트, 알림 발송 등
    }
} 