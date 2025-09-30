package com.sage.bif.diary.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class BifAccessForbiddenException extends BaseException {

    public BifAccessForbiddenException() {
        super(ErrorCode.COMMON_FORBIDDEN, "BIF 계정에 대한 접근 권한이 없습니다.");
    }

    public BifAccessForbiddenException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
