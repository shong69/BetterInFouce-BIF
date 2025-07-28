package com.sage.bif.common.constants;

public class ApiConstants {
    
    // API Paths
    public static final String API_BASE_PATH = "/api";
    public static final String API_VERSION = "/v1";
    
    // User API
    public static final String USER_API = "/users";
    public static final String USER_REGISTER = "/register";
    public static final String USER_LOGIN = "/login";
    public static final String USER_PROFILE = "/profile";
    
    // Todo API
    public static final String TODO_API = "/todos";
    public static final String TODO_COMPLETE = "/{id}/complete";
    
    // Diary API
    public static final String DIARY_API = "/diaries";
    public static final String DIARY_FEEDBACK = "/{id}/feedback";
    
    // Simulation API
    public static final String SIMULATION_API = "/simulations";
    public static final String SIMULATION_STEP = "/{id}/steps";
    
    // Stats API
    public static final String STATS_API = "/stats";
    public static final String STATS_USER = "/user/{userId}";
    
    // Common Headers
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    
    // Pagination
    public static final String PAGE_PARAM = "page";
    public static final String SIZE_PARAM = "size";
    public static final String SORT_PARAM = "sort";
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
} 