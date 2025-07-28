package com.sage.bif.diary.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import com.sage.bif.diary.entity.Diary;
import lombok.Getter;

@Getter
public class DiaryFeedbackRequestedEvent extends BaseEvent {
    
    private final Diary diary;
    private final Long userId;
    private final String feedbackType;
    private final String requestReason;
    
    public DiaryFeedbackRequestedEvent(Object source, Diary diary, Long userId, String feedbackType, String requestReason) {
        super(source);
        this.diary = diary;
        this.userId = userId;
        this.feedbackType = feedbackType;
        this.requestReason = requestReason;
    }
    
    public DiaryFeedbackRequestedEvent(Object source, Diary diary, Long userId, String feedbackType, String requestReason, String correlationId) {
        super(source, correlationId);
        this.diary = diary;
        this.userId = userId;
        this.feedbackType = feedbackType;
        this.requestReason = requestReason;
    }
    
    @Override
    public String getEventType() {
        return "DIARY_FEEDBACK_REQUESTED";
    }
} 