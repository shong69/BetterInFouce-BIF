package com.sage.bif.diary.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import com.sage.bif.diary.entity.Diary;
import lombok.Getter;

@Getter
public class DiaryUpdatedEvent extends BaseEvent {

    private final Diary diary;
    private final String previousContent;

    public DiaryUpdatedEvent(Object source, Diary diary, String previousContent) {
        super(source);
        this.diary = diary;
        this.previousContent = previousContent;
    }

    public DiaryUpdatedEvent(Object source, Diary diary, String previousContent, String correlationId) {
        super(source, correlationId);
        this.diary = diary;
        this.previousContent = previousContent;
    }

    @Override
    public String getEventType() {
        return "DIARY_UPDATED";
    }

}
