package com.sage.bif.notification.exception;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

public class WebPushConfigurationException extends BaseException {

    public WebPushConfigurationException(String message) {
        super(ErrorCode.COMMON_INTERNAL_SERVER_ERROR, message);
    }

}
