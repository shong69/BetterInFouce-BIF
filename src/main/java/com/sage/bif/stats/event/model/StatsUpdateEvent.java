package com.sage.bif.stats.event.model;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class StatsUpdateEvent extends ApplicationEvent {
    
    public enum EventType {
        DIARY_CREATED,
        DIARY_UPDATED,
        DIARY_DELETED
    }
    
    private final Long bifId;
    private final String content;
    private final EventType eventType;
    
    public StatsUpdateEvent(Object source, Long bifId, String content, EventType eventType) {
        super(source);
        this.bifId = bifId;
        this.content = content;
        this.eventType = eventType;
    }

}
