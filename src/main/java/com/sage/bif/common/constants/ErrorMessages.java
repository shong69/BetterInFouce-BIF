package com.sage.bif.common.constants;

public class ErrorMessages {
    
    // Common Error Messages
    public static final String INTERNAL_SERVER_ERROR = "Internal server error occurred";
    public static final String INVALID_INPUT = "Invalid input provided";
    public static final String RESOURCE_NOT_FOUND = "Requested resource not found";
    public static final String UNAUTHORIZED = "Unauthorized access";
    public static final String FORBIDDEN = "Access forbidden";
    public static final String VALIDATION_ERROR = "Validation error";
    
    // User Error Messages
    public static final String USER_NOT_FOUND = "User not found";
    public static final String USER_ALREADY_EXISTS = "User already exists";
    public static final String INVALID_CREDENTIALS = "Invalid credentials";
    public static final String EMAIL_ALREADY_EXISTS = "Email already exists";
    public static final String PASSWORD_MISMATCH = "Password mismatch";
    
    // Todo Error Messages
    public static final String TODO_NOT_FOUND = "Todo not found";
    public static final String TODO_ALREADY_COMPLETED = "Todo already completed";
    public static final String TODO_ACCESS_DENIED = "Access denied to this todo";
    
    // Diary Error Messages
    public static final String DIARY_NOT_FOUND = "Diary not found";
    public static final String DIARY_ALREADY_EXISTS = "Diary already exists for this date";
    public static final String DIARY_ACCESS_DENIED = "Access denied to this diary";
    
    // Simulation Error Messages
    public static final String SIMULATION_NOT_FOUND = "Simulation not found";
    public static final String SIMULATION_ALREADY_RUNNING = "Simulation already running";
    public static final String SIMULATION_ACCESS_DENIED = "Access denied to this simulation";
    
    // Stats Error Messages
    public static final String STATS_NOT_FOUND = "Stats not found";
    public static final String STATS_UPDATE_FAILED = "Stats update failed";
    public static final String STATS_ACCESS_DENIED = "Access denied to stats";
    
    // AI Service Error Messages
    public static final String AI_SERVICE_UNAVAILABLE = "AI service is currently unavailable";
    public static final String AI_PROCESSING_FAILED = "AI processing failed";
    public static final String AI_RATE_LIMIT_EXCEEDED = "AI rate limit exceeded";
} 