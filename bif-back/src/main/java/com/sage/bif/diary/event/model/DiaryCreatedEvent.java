package com.sage.bif.diary.event.model;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DiaryCreatedEvent extends ApplicationEvent {
    
    private final Long bifId;
    private final String content;
    private final Long diaryId;
    
    public DiaryCreatedEvent(Object source, Long bifId, String content, Long diaryId) {
        super(source);
        this.bifId = bifId;
        this.content = content;
        this.diaryId = diaryId;
    }
}
