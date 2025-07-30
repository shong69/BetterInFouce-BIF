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
    }
    
    @Async
    @EventListener
    public void handleSimulationStepCompletedAsync(SimulationStepCompletedEvent event) {
        log.info("Async processing simulation step completion: {} - User: {} - Step: {} - EventId: {}", 
                event.getSimulation().getId(), event.getUserId(), event.getStepName(), event.getEventId());
    }
} 