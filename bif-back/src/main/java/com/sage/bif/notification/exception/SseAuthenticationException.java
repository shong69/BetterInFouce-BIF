package com.sage.bif.notification.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class SseAuthenticationException extends BaseException {

    public SseAuthenticationException() {
        super(ErrorCode.COMMON_UNAUTHORIZED, "SSE 연결을 위한 인증이 필요합니다.");
    }

}
