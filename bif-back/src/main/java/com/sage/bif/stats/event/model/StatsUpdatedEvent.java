package com.sage.bif.stats.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import com.sage.bif.stats.entity.Stats;
import lombok.Getter;

@Getter
public class StatsUpdatedEvent extends BaseEvent {

    private final transient Stats stats;
    private final Long userId;
    private final String updateType;
    private final String updateReason;

    public StatsUpdatedEvent(final Object source, final Stats stats, final Long userId, final String updateType, final String updateReason) {
        super(source);
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
