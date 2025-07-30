package com.sage.bif.diary.dto;

/**
 * 일기 기능을 위한 AI 설정
 */
public class AiDiarySettings {
    
    // 일기 피드백 생성 설정
    public static final double FEEDBACK_TEMPERATURE = 0.3;  // 일관성 있는 피드백
    public static final int FEEDBACK_MAX_TOKENS = 500;
    public static final String FEEDBACK_SYSTEM_PROMPT = 
        "당신은 따뜻하고 격려적인 일기 피드백 전문가입니다. " +
        "사용자의 일기를 읽고 공감적이고 건설적인 피드백을 제공하세요. " +
        "감정을 인정하고 긍정적인 관점을 제시하며, 개선점을 부드럽게 제안하세요.";
    
    // 감정 분석 설정
    public static final double EMOTION_ANALYSIS_TEMPERATURE = 0.1;  // 정확한 분석
    public static final int EMOTION_ANALYSIS_MAX_TOKENS = 200;
    public static final String EMOTION_ANALYSIS_SYSTEM_PROMPT = 
        "당신은 정확한 감정 분석 전문가입니다. " +
        "일기 내용에서 주요 감정을 객관적으로 분석하고, " +
        "감정 강도와 변화를 파악하여 JSON 형태로 응답하세요.";
    
    // 일기 요약 설정
    public static final double SUMMARY_TEMPERATURE = 0.2;  // 일관된 요약
    public static final int SUMMARY_MAX_TOKENS = 300;
    public static final String SUMMARY_SYSTEM_PROMPT = 
        "당신은 일기 요약 전문가입니다. " +
        "일기 내용의 핵심을 간결하고 명확하게 요약하세요. " +
        "주요 사건, 감정, 인사이트를 포함하여 구조화된 요약을 제공하세요.";
} 