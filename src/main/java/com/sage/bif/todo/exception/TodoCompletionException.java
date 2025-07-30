package com.sage.bif.todo.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class TodoCompletionException extends BaseException {

    public TodoCompletionException(Long todoId) {

        super(ErrorCode.SUBTODOS_NOT_COMPLETED, "Cannot complete Todo " + todoId + ": not all SubTodos are completed");

    }

    public TodoCompletionException() {

        super(ErrorCode.SUBTODOS_NOT_COMPLETED);

    }

    public TodoCompletionException(ErrorCode errorCode, String message) {

        super(errorCode, message);

    }

}