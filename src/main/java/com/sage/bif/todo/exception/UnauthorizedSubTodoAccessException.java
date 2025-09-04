package com.sage.bif.todo.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class UnauthorizedSubTodoAccessException extends BaseException {

    public UnauthorizedSubTodoAccessException(Long subTodoId) {
        super(ErrorCode.SUBTODO_ACCESS_DENIED,
                String.format("세부 할일 %d에 접근할 권한이 없습니다.", subTodoId));
    }

    public UnauthorizedSubTodoAccessException() {
        super(ErrorCode.SUBTODO_ACCESS_DENIED);
    }

}
