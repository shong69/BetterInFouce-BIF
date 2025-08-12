package com.sage.bif.diary.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import lombok.Getter;

@Getter
public class DiaryDeletedEvent extends BaseEvent {

    private final Long diaryId;
    private final Long userId;
    private final String deletedContent;
    private final String emotion;

    public DiaryDeletedEvent(Object source, Long diaryId, Long userId, String deletedContent, String emotion) {
        super(source);
        this.diaryId = diaryId;
        this.userId = userId;
        this.deletedContent = deletedContent;
        this.emotion = emotion;
    }

    public DiaryDeletedEvent(Object source, Long diaryId, Long userId, String deletedContent, String emotion, String correlationId) {
        super(source, correlationId);
        this.diaryId = diaryId;
        this.userId = userId;
        this.deletedContent = deletedContent;
        this.emotion = emotion;
    }

    @Override
    public String getEventType() {
        return "DIARY_DELETED";
    }

}
