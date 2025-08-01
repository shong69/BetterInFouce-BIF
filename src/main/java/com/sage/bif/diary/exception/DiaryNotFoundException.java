package com.sage.bif.diary.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class DiaryNotFoundException extends BaseException {
    
    public DiaryNotFoundException(Long diaryId) {
        super(ErrorCode.DIARY_NOT_FOUND, "Diary not found with id: " + diaryId);
    }
    
    public DiaryNotFoundException(Long userId, Long diaryId) {
        super(ErrorCode.DIARY_NOT_FOUND, "Diary not found with id: " + diaryId + " for user: " + userId);
    }
    
    public DiaryNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}