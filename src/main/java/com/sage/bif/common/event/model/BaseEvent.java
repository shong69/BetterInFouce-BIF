package com.sage.bif.common.event.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public abstract class BaseEvent extends ApplicationEvent {

    private final String eventId = UUID.randomUUID().toString();
    private final LocalDateTime eventTimestamp = LocalDateTime.now();
    private String source;
    private String correlationId;

    public BaseEvent(Object source) {
        super(source);
        this.source = source.getClass().getSimpleName();
    }

    public BaseEvent(Object source, String correlationId) {
        super(source);
        this.source = source.getClass().getSimpleName();
        this.correlationId = correlationId;
    }

    public abstract String getEventType();

}
