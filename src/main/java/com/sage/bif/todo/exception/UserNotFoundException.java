package com.sage.bif.todo.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class UserNotFoundException extends BaseException {

    public UserNotFoundException(Long userId) {
        super(ErrorCode.USER_NOT_FOUND, String.format("사용자를 찾을 수 없습니다. ID: %d", userId));
    }

    public UserNotFoundException(Long userId, boolean includeId) {
        super(ErrorCode.USER_NOT_FOUND,
                includeId ?
                        String.format("사용자를 찾을 수 없습니다. ID: %d", userId) :
                        "사용자를 찾을 수 없습니다.");
    }

    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }

}
