package com.sage.bif.todo.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class TodoCompletionException extends BaseException {

    public TodoCompletionException(Long todoId) {
        super(ErrorCode.SUBTODOS_NOT_COMPLETED,
                String.format("할일 %d를 완료할 수 없습니다. 모든 세부 할일을 먼저 완료해주세요.", todoId));
    }

    public TodoCompletionException(Long todoId, int incompletedCount) {
        super(ErrorCode.SUBTODOS_NOT_COMPLETED,
                String.format("할일 %d를 완료할 수 없습니다. %d개의 세부 할일이 미완료 상태입니다.", todoId, incompletedCount));
    }

    public TodoCompletionException() {
        super(ErrorCode.SUBTODOS_NOT_COMPLETED);
    }

}
