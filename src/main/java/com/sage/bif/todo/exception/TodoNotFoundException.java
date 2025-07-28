package com.sage.bif.todo.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class TodoNotFoundException extends BaseException {
    
    public TodoNotFoundException(Long todoId) {
        super(ErrorCode.TODO_NOT_FOUND, "Todo not found with id: " + todoId);
    }
    
    public TodoNotFoundException(Long userId, Long todoId) {
        super(ErrorCode.TODO_NOT_FOUND, "Todo not found with id: " + todoId + " for user: " + userId);
    }
    
    public TodoNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
} 