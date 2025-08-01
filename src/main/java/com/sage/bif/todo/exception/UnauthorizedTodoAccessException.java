package com.sage.bif.todo.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class UnauthorizedTodoAccessException extends BaseException {
    
    public UnauthorizedTodoAccessException(Long bifId, Long todoId) {

        super(ErrorCode.TODO_ACCESS_DENIED, "User " + bifId + " does not have permission to access Todo " + todoId);

    }
    
    public UnauthorizedTodoAccessException() {

        super(ErrorCode.TODO_ACCESS_DENIED);

    }
    
    public UnauthorizedTodoAccessException(ErrorCode errorCode, String message) {

        super(errorCode, message);

    }

}