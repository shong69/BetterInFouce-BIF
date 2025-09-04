package com.sage.bif.todo.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class SubTodoNotFoundException extends BaseException {

    public SubTodoNotFoundException(Long subTodoId) {
        super(ErrorCode.SUBTODO_NOT_FOUND,
                String.format("세부 할일을 찾을 수 없습니다. ID: %d", subTodoId));
    }

    public SubTodoNotFoundException(Long bifId, Long subTodoId) {
        super(ErrorCode.SUBTODO_NOT_FOUND,
                String.format("사용자 %d의 세부 할일 %d를 찾을 수 없습니다.", bifId, subTodoId));
    }

    public SubTodoNotFoundException() {
        super(ErrorCode.SUBTODO_NOT_FOUND);
    }

}
