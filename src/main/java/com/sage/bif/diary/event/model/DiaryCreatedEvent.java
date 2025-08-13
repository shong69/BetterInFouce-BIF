package com.sage.bif.diary.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import com.sage.bif.diary.entity.Diary;
import lombok.Getter;

@Getter
public class DiaryCreatedEvent extends BaseEvent {

    private final Diary diary;

    public DiaryCreatedEvent(Object source, Diary diary) {
        super(source);
        this.diary = diary;
    }

    public DiaryCreatedEvent(Object source, Diary diary, String correlationId) {
        super(source, correlationId);
        this.diary = diary;
    }

    @Override
    public String getEventType() {
        return "DIARY_CREATED";
    }

}
