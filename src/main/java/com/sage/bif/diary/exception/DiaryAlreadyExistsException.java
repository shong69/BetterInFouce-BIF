package com.sage.bif.diary.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

import java.time.LocalDate;

public class DiaryAlreadyExistsException extends BaseException {

    public DiaryAlreadyExistsException(String message) {
        super(ErrorCode.DIARY_ALREADY_EXISTS, message);
    }

    public DiaryAlreadyExistsException(LocalDate date) {
        super(ErrorCode.DIARY_ALREADY_EXISTS,
                "해당 날짜(" + date + ")에 이미 일기가 존재합니다.");
    }

}
