package com.sage.bif.todo.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class UnauthorizedSubTodoAccessException extends BaseException {

    public UnauthorizedSubTodoAccessException(Long bifId, Long subTodoId) {

        super(ErrorCode.SUBTODO_ACCESS_DENIED, "User " + bifId + " does not have permission to access SubTodo " + subTodoId);

    }

    public UnauthorizedSubTodoAccessException() {

        super(ErrorCode.SUBTODO_ACCESS_DENIED);

    }

    public UnauthorizedSubTodoAccessException(ErrorCode errorCode, String message) {

        super(errorCode, message);

    }

}