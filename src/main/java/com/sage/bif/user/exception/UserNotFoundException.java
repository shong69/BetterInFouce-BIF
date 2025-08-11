package com.sage.bif.user.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class UserNotFoundException extends BaseException {

    public UserNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
