package com.sage.bif.diary.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class DiaryNotFoundException extends BaseException {

    public DiaryNotFoundException(Long diaryId) {
        super(ErrorCode.DIARY_NOT_FOUND, "해당 일기가 존재하지 않습니다. 일기 ID: " + diaryId);
    }

    public DiaryNotFoundException(Long userId, Long diaryId) {
        super(ErrorCode.DIARY_NOT_FOUND, "해당 일기가 존재하지 않습니다. 일기 ID: " + diaryId + " 사용자 ID: " + userId);
    }

    public DiaryNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
