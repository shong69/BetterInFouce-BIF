package com.sage.bif.todo.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class AiResponseParsingException extends BaseException {

    public AiResponseParsingException(String message, Throwable cause) {
        super(ErrorCode.TODO_AI_GENERATION_FAILED,
                String.format("AI 응답 파싱 실패: %s", message), cause);
    }

    public AiResponseParsingException(String message) {
        super(ErrorCode.TODO_AI_GENERATION_FAILED,
                String.format("AI 응답 파싱 실패: %s", message));
    }

    public AiResponseParsingException(Throwable cause) {
        super(ErrorCode.TODO_AI_GENERATION_FAILED,
                "AI 응답 파싱 중 오류가 발생했습니다.", cause);
    }

}
