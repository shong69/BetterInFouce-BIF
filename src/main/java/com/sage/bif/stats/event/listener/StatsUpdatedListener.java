package com.sage.bif.stats.event.listener;

import com.sage.bif.stats.event.model.StatsUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StatsUpdatedListener {

    @Async
    @EventListener
    public void handleStatsUpdated(final StatsUpdatedEvent event) {
        log.info("Stats updated: {} - User: {} - Type: {} - Reason: {} - EventId: {}",
                event.getStats().getId(), event.getUserId(), event.getUpdateType(),
                event.getUpdateReason(), event.getEventId());
    }

}
