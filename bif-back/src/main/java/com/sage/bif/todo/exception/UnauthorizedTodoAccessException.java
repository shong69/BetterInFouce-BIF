package com.sage.bif.todo.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class UnauthorizedTodoAccessException extends BaseException {

    public UnauthorizedTodoAccessException(Long todoId) {
        super(ErrorCode.TODO_ACCESS_DENIED,
                String.format("할일 %d에 접근할 권한이 없습니다.", todoId));
    }

    public UnauthorizedTodoAccessException(Long bifId, Long todoId, boolean includeUserInfo) {
        super(ErrorCode.TODO_ACCESS_DENIED,
                includeUserInfo ?
                        String.format("사용자 %d가 할일 %d에 접근할 권한이 없습니다.", bifId, todoId) :
                        String.format("할일 %d에 접근할 권한이 없습니다.", todoId));
    }

    public UnauthorizedTodoAccessException() {
        super(ErrorCode.TODO_ACCESS_DENIED);
    }

}
