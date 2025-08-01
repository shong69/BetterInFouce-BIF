package com.sage.bif.todo.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class UserNotFoundException extends BaseException {
    
    public UserNotFoundException(Long userId) {

        super(ErrorCode.USER_NOT_FOUND, "User not found with id: " + userId);

    }
    
    public UserNotFoundException() {

        super(ErrorCode.USER_NOT_FOUND);

    }
    
    public UserNotFoundException(ErrorCode errorCode, String message) {

        super(errorCode, message);

    }

}