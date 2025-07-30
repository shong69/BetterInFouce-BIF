package com.sage.bif.diary.service;

import com.sage.bif.common.client.ai.AiServiceClient;
import com.sage.bif.common.client.ai.dto.AiRequest;
import com.sage.bif.common.client.ai.dto.AiResponse;
import com.sage.bif.common.client.ai.AiSettings;
import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
import com.sage.bif.diary.dto.request.DiaryCreateRequest;
import com.sage.bif.diary.dto.response.DiaryResponse;
import com.sage.bif.diary.entity.Diary;
import com.sage.bif.diary.repository.DiaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 일기 서비스 구현체
 * HTTP 직접 호출 방식을 사용하여 AI 기능 제공
 */
@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {
    
    private final DiaryRepository diaryRepository;
    private final AiServiceClient aiModelClient;
    
    @Override
    public DiaryResponse createDiary(DiaryCreateRequest request) {
        // 1. 일기 저장
        Diary diary = Diary.builder()
            .title(request.getTitle())
            .content(request.getContent())
            .userId(request.getUserId())
            .build();
        
        Diary savedDiary = diaryRepository.save(diary);
        
        // 2. AI 피드백 생성 (AiSettings 활용)
        String feedback = generateAiFeedback(request.getContent());
        
        return DiaryResponse.builder()
            .id(savedDiary.getId())
            .title(savedDiary.getTitle())
            .content(savedDiary.getContent())
            .userId(savedDiary.getUserId())
            .aiFeedback(feedback)
            .createdAt(savedDiary.getCreatedAt())
            .build();
    }
    
    /**
     * AI 피드백 생성 (AiSettings 활용)
     */
    private String generateAiFeedback(String content) {
        try {
            AiRequest request = new AiRequest(content);
            
            AiResponse response = aiModelClient.generate(request, AiSettings.DIARY_FEEDBACK);
            
            return response.getContent();
        } catch (BaseException e) {
            throw new BaseException(ErrorCode.DIARY_AI_FEEDBACK_FAILED, 
                "AI 피드백 생성 실패: " + e.getMessage());
        }
    }
} 