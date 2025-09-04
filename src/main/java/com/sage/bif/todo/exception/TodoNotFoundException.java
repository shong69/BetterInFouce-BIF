package com.sage.bif.todo.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class TodoNotFoundException extends BaseException {

    public TodoNotFoundException(Long todoId) {
        super(ErrorCode.TODO_NOT_FOUND, String.format("할 일을 찾을 수 없습니다. ID: %d", todoId));
    }

    public TodoNotFoundException(Long bifId, Long todoId) {
        super(ErrorCode.TODO_NOT_FOUND, String.format("사용자 %d의 할 일 %d를 찾을 수 없습니다.", bifId, todoId));
    }

    public TodoNotFoundException() {
        super(ErrorCode.TODO_NOT_FOUND);
    }

}
