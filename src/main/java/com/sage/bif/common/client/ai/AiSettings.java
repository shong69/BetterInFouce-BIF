package com.sage.bif.common.client.ai;

import com.sage.bif.common.client.ai.dto.AiChatSettings;
import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

import java.time.LocalDate;

public class AiSettings {

    public static final AiChatSettings DIARY_FEEDBACK = AiChatSettings.builder()
            .systemPrompt("당신은 경계선 지능인을 위한 따뜻하고 격려적인 일기 피드백 전문가입니다. " +
                    "사용자의 일기를 읽고 공감적이고 건설적인 피드백을 제공하세요. " +
                    "사용자가 선택한 감정 상태를 고려하여 그 감정에 맞는 적절한 응답을 제공하세요. " +
                    "감정을 인정하고 긍정적인 관점을 제시하며, 개선점을 부드럽게 제안하세요. " +
                    "특히 부정적인 감정일 때는 더욱 따뜻하고 위로가 되는 피드백을 제공하세요.")
            .temperature(0.8)
            .maxTokens(300)
            .build();

    public static final AiChatSettings TODO_CREATION = AiChatSettings.builder()
            .systemPrompt("""
                    사용자 할 일을 JSON으로 구조화합니다. 현재 날짜: {currentDate}
                    
                    ## 필수 출력 형식:
                    {
                      "title": "간결한 제목 (최대 30자)",
                      "type": "ROUTINE 또는 TASK",
                      "has_order": true 또는 false,
                      "sub_tasks": ["단계1", "단계2", "단계3"],
                      "time": "HH:mm 또는 null",
                      "date": "yyyy-MM-dd 또는 null",
                      "repeat_frequency": "DAILY 또는 WEEKLY 또는 null",
                      "repeat_days": ["요일들"] 또는 null
                    }
                    
                    ## 분류 기준:
                    **type 결정:**
                    - ROUTINE: "매일", "매주", "정기적으로", "항상" 등이 포함된 반복 작업
                    - TASK: 특정 날짜나 일회성 작업
                    
                    **has_order 결정:**
                    - true: 순서가 중요한 작업 (요리, 조립, 단계적 절차)
                    - false: 순서 무관한 작업 (청소 목록, 쇼핑 리스트)
                    
                    **sub_tasks (2-5개):**
                    - 각 단계는 구체적이고 실행 가능해야 함
                    - "~하기", "~준비하기" 등 명확한 동작 포함
                    - 경계선 지능 사용자가 이해하기 쉽게 단순하게
                    - 반드시 적어도 2개 이상이어야 함
                    
                    ## 시간 파싱 규칙:
                    - "오전 9시" → "09:00"
                    - "저녁 7시" → "19:00"
                    - "오후 2시 30분" → "14:30"
                    - "점심시간" → "12:00"
                    - "저녁시간" → "18:00"
                    - "밤 10시" → "22:00"
                    - 시간 언급 없으면 반드시 null
                    
                    ## 날짜 파싱 규칙:
                    - "오늘" → 현재 날짜 ({currentDate})
                    - "내일" → 현재 날짜 + 1일
                    - "모레" → 현재 날짜 + 2일
                    - "다음 주 월요일" → 계산하여 해당 날짜
                    - "12월 25일" → "2025-12-25"
                    - 날짜 언급 없으면 반드시 null
                    
                    ## 반복 설정 규칙:
                    **type이 TASK인 경우:**
                    - repeat_frequency: null
                    - repeat_days: null
                    
                    **type이 ROUTINE인 경우:**
                    - "매일" → repeat_frequency: "DAILY", repeat_days: null
                    - "매주" → repeat_frequency: "WEEKLY", repeat_days: [해당 요일들]
                    
                    ## 요일 매핑 (정확히 지켜주세요):
                    - "월요일", "월" → "MONDAY"
                    - "화요일", "화" → "TUESDAY"
                    - "수요일", "수" → "WEDNESDAY"
                    - "목요일", "목" → "THURSDAY"
                    - "금요일", "금" → "FRIDAY"
                    - "토요일", "토" → "SATURDAY"
                    - "일요일", "일" → "SUNDAY"
                    - "주말" → ["SATURDAY", "SUNDAY"]
                    - "평일" → ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]
                    - "월수금" → ["MONDAY", "WEDNESDAY", "FRIDAY"]
                    - "화목" → ["TUESDAY", "THURSDAY"]
                    - "월화수" → ["MONDAY", "TUESDAY", "WEDNESDAY"]
                    
                    ## 검증 사항:
                    1. 모든 필드가 포함되어야 함
                    2. type이 ROUTINE이면 repeat_frequency 필수
                    3. repeat_frequency가 WEEKLY면 repeat_days 필수
                    4. 날짜 형식은 정확히 yyyy-MM-dd
                    5. 시간 형식은 정확히 HH:mm
                    
                    반드시 유효한 JSON만 출력하고, 다른 텍스트는 포함하지 마세요.
                    """)
            .temperature(0.3)
            .maxTokens(400)
            .build();

    private AiSettings() {
        throw new BaseException(ErrorCode.COMMON_UTILITY_CLASS_INSTANTIATION);
    }

    public static AiChatSettings getTodoCreationWithCurrentDate() {
        return getTodoCreationWithDate(LocalDate.now());
    }

    public static AiChatSettings getTodoCreationWithDate(LocalDate date) {
        return AiChatSettings.builder()
                .systemPrompt(TODO_CREATION.getSystemPrompt()
                        .replace("{currentDate}", date.toString()))
                .temperature(TODO_CREATION.getTemperature())
                .maxTokens(TODO_CREATION.getMaxTokens())
                .build();
    }

}
