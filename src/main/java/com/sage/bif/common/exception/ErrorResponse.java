package com.sage.bif.common.exception;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Getter
@Builder
public class ErrorResponse {
    private final String code;
    private final String message;
    private final Object details;
    private final String timestamp;

    public ErrorResponse(String code, String message, Object details) {
        this.code = code;
        this.message = message;
        this.details = details;
        this.timestamp = LocalDateTime.now(ZoneOffset.UTC).toString();
    }
}