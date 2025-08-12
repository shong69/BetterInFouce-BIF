package com.sage.bif.todo.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class InvalidSubTodoRelationException extends BaseException {

    public InvalidSubTodoRelationException(Long subTodoId, Long todoId) {
        super(ErrorCode.SUBTODO_INVALID_RELATION,
                String.format("세부 할일 %d가 할일 %d에 속하지 않습니다.", subTodoId, todoId));
    }

    public InvalidSubTodoRelationException(Long subTodoId, Long todoId, String additionalMessage) {
        super(ErrorCode.SUBTODO_INVALID_RELATION,
                String.format("세부 할일 %d가 할일 %d에 속하지 않습니다. %s", subTodoId, todoId, additionalMessage));
    }

    public InvalidSubTodoRelationException() {
        super(ErrorCode.SUBTODO_INVALID_RELATION);
    }

    public InvalidSubTodoRelationException(String customMessage) {
        super(ErrorCode.SUBTODO_INVALID_RELATION, customMessage);
    }

}
