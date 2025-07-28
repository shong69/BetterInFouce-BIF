package com.sage.bif.stats.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import com.sage.bif.stats.entity.Stats;
import lombok.Getter;

@Getter
public class StatsUpdatedEvent extends BaseEvent {
    
    private final Stats stats;
    private final Long userId;
    private final String updateType;
    private final String updateReason;
    
    public StatsUpdatedEvent(Object source, Stats stats, Long userId, String updateType, String updateReason) {
        super(source);
        this.stats = stats;
        this.userId = userId;
        this.updateType = updateType;
        this.updateReason = updateReason;
    }
    
    public StatsUpdatedEvent(Object source, Stats stats, Long userId, String updateType, String updateReason, String correlationId) {
        super(source, correlationId);
        this.stats = stats;
        this.userId = userId;
        this.updateType = updateType;
        this.updateReason = updateReason;
    }
    
    @Override
    public String getEventType() {
        return "STATS_UPDATED";
    }
} 