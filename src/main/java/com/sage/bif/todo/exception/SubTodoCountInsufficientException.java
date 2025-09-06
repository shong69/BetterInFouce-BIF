package com.sage.bif.todo.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class SubTodoCountInsufficientException extends BaseException {

    public SubTodoCountInsufficientException() {
        super(ErrorCode.SUBTODO_COUNT_INSUFFICIENT);
    }

    public SubTodoCountInsufficientException(String message) {
        super(ErrorCode.SUBTODO_COUNT_INSUFFICIENT, message);
    }

}
