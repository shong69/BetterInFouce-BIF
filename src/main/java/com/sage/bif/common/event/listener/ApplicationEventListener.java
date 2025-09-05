package com.sage.bif.common.event.listener;

import com.sage.bif.common.event.model.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApplicationEventListener {

    @Async
    @EventListener
    public void handleBaseEvent(BaseEvent event) {
        log.info("Event received: {} - EventId: {} - Source: {} - CorrelationId: {}",
                event.getEventType(), event.getEventId(), event.getSource(), event.getCorrelationId());
    }

}
