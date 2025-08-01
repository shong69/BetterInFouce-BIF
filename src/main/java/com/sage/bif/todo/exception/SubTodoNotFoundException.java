package com.sage.bif.todo.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class SubTodoNotFoundException extends BaseException {

    public SubTodoNotFoundException(Long subTodoId) {

        super(ErrorCode.SUBTODO_NOT_FOUND, "SubTodo not found with id: " + subTodoId);

    }

    public SubTodoNotFoundException(Long bifId, Long subTodoId) {

        super(ErrorCode.SUBTODO_NOT_FOUND, "SubTodo not found with id: " + subTodoId + " for bif: " + bifId);

    }

    public SubTodoNotFoundException(ErrorCode errorCode, String message) {

        super(errorCode, message);

    }

}
