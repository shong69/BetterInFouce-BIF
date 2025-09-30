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
    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now(ZoneOffset.UTC);

}
