package com.sage.bif.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    // Common Errors
    INTERNAL_SERVER_ERROR("E001", "Internal server error"),
    INVALID_INPUT("E002", "Invalid input"),
    RESOURCE_NOT_FOUND("E003", "Resource not found"),
    UNAUTHORIZED("E004", "Unauthorized"),
    FORBIDDEN("E005", "Forbidden"),
    
    // User Domain Errors
    USER_NOT_FOUND("U001", "User not found"),
    USER_ALREADY_EXISTS("U002", "User already exists"),
    INVALID_USER_CREDENTIALS("U003", "Invalid user credentials"),
    
    // Todo Domain Errors
    TODO_NOT_FOUND("T001", "Todo not found"),
    TODO_ALREADY_COMPLETED("T002", "Todo already completed"),
    
    // Diary Domain Errors
    DIARY_NOT_FOUND("D001", "Diary not found"),
    DIARY_ALREADY_EXISTS("D002", "Diary already exists for this date"),
    
    // Simulation Domain Errors
    SIMULATION_NOT_FOUND("S001", "Simulation not found"),
    SIMULATION_ALREADY_RUNNING("S002", "Simulation already running"),
    
    // Stats Domain Errors
    STATS_NOT_FOUND("ST001", "Stats not found"),
    STATS_UPDATE_FAILED("ST002", "Stats update failed");
    
    private final String code;
    private final String message;
} 