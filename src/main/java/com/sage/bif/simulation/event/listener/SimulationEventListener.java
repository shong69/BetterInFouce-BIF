package com.sage.bif.simulation.event.listener;

import com.sage.bif.simulation.event.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SimulationEventListener {
    
    // 시뮬레이션 시작 이벤트 처리
    @EventListener
    public void handleSimulationStarted(SimulationStartedEvent event) {
        log.info("Simulation started: {} - Session: {} - User: {} - Title: {} - EventId: {}", 
                event.getSimulationId(), event.getSessionId(), event.getUserId(), 
                event.getSimulationTitle(), event.getEventId());
        
        // TODO: 분석 서비스로 이벤트 전송 (MSA 전환 시)
        // analyticsService.trackSimulationStart(event);
    }
    
    // 선택지 제출 이벤트 처리
    @EventListener
    public void handleSimulationChoiceSubmitted(SimulationChoiceSubmittedEvent event) {
        log.info("Simulation choice submitted: {} - Session: {} - Choice: {} - Step: {} - Score: {} - EventId: {}", 
                event.getSimulationId(), event.getSessionId(), event.getSelectedChoice(), 
                event.getCurrentStep(), event.getCurrentScore(), event.getEventId());
        
        // TODO: 분석 서비스로 이벤트 전송 (MSA 전환 시)
        // analyticsService.trackChoiceSubmission(event);
    }
    
    // 시뮬레이션 완료 이벤트 처리
    @EventListener
    public void handleSimulationCompleted(SimulationCompletedEvent event) {
        log.info("Simulation completed: {} - Session: {} - Final Score: {} - Grade: {} - EventId: {}", 
                event.getSimulationId(), event.getSessionId(), event.getFinalScore(), 
                event.getFinalGrade(), event.getEventId());
        
        // TODO: 분석 서비스로 이벤트 전송 (MSA 전환 시)
        // analyticsService.trackSimulationCompletion(event);
    }
    
    // 비동기 처리 (MSA 전환 시 메시지 큐로 전송)
    @Async
    @EventListener
    public void handleSimulationStartedAsync(SimulationStartedEvent event) {
        log.info("Async processing simulation start: {} - Session: {} - EventId: {}", 
                event.getSimulationId(), event.getSessionId(), event.getEventId());
        
        // TODO: 메시지 큐로 이벤트 전송 (MSA 전환 시)
        // messageQueueService.publish("simulation.started", event);
    }
    
    @Async
    @EventListener
    public void handleSimulationChoiceSubmittedAsync(SimulationChoiceSubmittedEvent event) {
        log.info("Async processing choice submission: {} - Session: {} - EventId: {}", 
                event.getSimulationId(), event.getSessionId(), event.getEventId());
        
        // TODO: 메시지 큐로 이벤트 전송 (MSA 전환 시)
        // messageQueueService.publish("simulation.choice.submitted", event);
    }
    
    @Async
    @EventListener
    public void handleSimulationCompletedAsync(SimulationCompletedEvent event) {
        log.info("Async processing simulation completion: {} - Session: {} - EventId: {}", 
                event.getSimulationId(), event.getSessionId(), event.getEventId());
        
        // TODO: 메시지 큐로 이벤트 전송 (MSA 전환 시)
        // messageQueueService.publish("simulation.completed", event);
    }
} 