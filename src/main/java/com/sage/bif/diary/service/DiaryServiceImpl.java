package com.sage.bif.diary.service;

import com.sage.bif.common.client.ai.AiServiceClient;
import com.sage.bif.common.client.ai.dto.AiRequest;
import com.sage.bif.common.client.ai.dto.AiResponse;
import com.sage.bif.common.client.ai.AiSettings;
import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
import com.sage.bif.diary.dto.request.DiaryRequest;
import com.sage.bif.diary.dto.request.MonthlySummaryRequest;
import com.sage.bif.diary.dto.response.DiaryResponse;
import com.sage.bif.diary.dto.response.MonthlySummaryResponse;
import com.sage.bif.diary.entity.AiFeedback;
import com.sage.bif.diary.entity.Diary;
import com.sage.bif.diary.exception.DiaryNotFoundException;
import com.sage.bif.diary.exception.BifAccessForbiddenException;
import com.sage.bif.diary.exception.DiaryAlreadyExistsException;
import com.sage.bif.diary.model.Emotion;
import com.sage.bif.diary.repository.AiFeedbackRepository;
import com.sage.bif.diary.repository.DiaryRepository;
import com.sage.bif.user.entity.Bif;
import com.sage.bif.user.repository.BifRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.sage.bif.common.exception.ErrorCode.COMMON_FORBIDDEN;
import static com.sage.bif.common.exception.ErrorCode.DIARY_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {
    
    private final DiaryRepository diaryRepository;
    private final AiFeedbackRepository aiFeedbackRepository;
    private final BifRepository bifRepository;
    private final AiServiceClient aiModelClient;


    @Override
    public MonthlySummaryResponse getMonthlySummary(Long bifId, MonthlySummaryRequest request) {

        LocalDate startDate = LocalDate.of(request.getYear(), request.getMonth(),1);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = start.plusMonths(1);

        List<Diary> diaries = diaryRepository.findByUserIdAndDateBetween(bifId,start,end);
        if(diaries.isEmpty()){
            return MonthlySummaryResponse.builder()
                    .year(request.getYear())
                    .month(request.getMonth())
                    .dailyEmotions(Collections.emptyMap())
                    .diarySummaries(Collections.emptyList())
                    .build();
        }
        Map<LocalDate, Emotion> dailyEmotions = diaries.stream()
                .collect(Collectors.toMap(
                        diary -> diary.getCreatedAt().toLocalDate(),
                        Diary::getEmotion
                ));

        List<MonthlySummaryResponse.DiarySummary> diarySummaries = diaries.stream()
                .map(diary -> MonthlySummaryResponse.DiarySummary.builder()
                        .id(diary.getId())
                        .date(diary.getCreatedAt().toLocalDate())
                        .emotion(diary.getEmotion())
                        .build())
                .toList();

        return MonthlySummaryResponse.builder()
                .year(request.getYear())
                .month(request.getMonth())
                .dailyEmotions(dailyEmotions)
                .diarySummaries(diarySummaries)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DiaryResponse getDiary(Long bifId, Long diaryId) {
        Diary diary = diaryRepository.findByIdWithUser(diaryId)
            .orElseThrow(() -> new DiaryNotFoundException(DIARY_NOT_FOUND, "일기를 찾을 수 없습니다."));

        // 기존 패턴과 일치하는 사용법
        if (!diary.getUser().getBifId().equals(bifId)) {
            throw new BifAccessForbiddenException(COMMON_FORBIDDEN, "일기에 대한 접근 권한이 없습니다.");
        }
        String aiFeedback = aiFeedbackRepository.findByDiaryId(diaryId)
            .map(AiFeedback::getContent)
            .orElse(null);

        return DiaryResponse.builder()
                .id(diary.getId())
                .content(diary.getContent())
                .userId(diary.getUser().getBifId())
                .emotion(diary.getEmotion())
                .aiFeedback(aiFeedback)
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public DiaryResponse createDiary(Long bifId, DiaryRequest request) {
        Bif user = bifRepository.findById(bifId)
        .orElseThrow(() -> new BaseException(ErrorCode.COMMON_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        boolean exists = diaryRepository.existsByUserIdAndDate(bifId, request.getDate().toLocalDate());
        
        if (exists) {
            throw new DiaryAlreadyExistsException(request.getDate().toLocalDate());
        }

        Diary diary = Diary.builder()
            .content(request.getContent())
            .user(user)
            .emotion(request.getEmotion())
            .createdAt(request.getDate())
            .isDeleted(false)
            .build();
        
        Diary savedDiary = diaryRepository.save(diary);

        AiFeedback feedback = AiFeedback.builder()
            .diary(savedDiary)
            .content(generateAiFeedback(request.getContent()))
            .build();

        aiFeedbackRepository.save(feedback);
        
        return DiaryResponse.builder()
            .id(savedDiary.getId())
            .content(savedDiary.getContent())
            .userId(bifId)
            .emotion(savedDiary.getEmotion())
            .aiFeedback(feedback.getContent())
            .createdAt(savedDiary.getCreatedAt())
            .updatedAt(savedDiary.getUpdatedAt())
            .build();
    }

    @Override
    @Transactional
    public DiaryResponse updateDiaryContent(Long bifId, Long diaryId, String content) {
        Diary diary = diaryRepository.findByIdWithUser(diaryId)
            .orElseThrow(() -> new DiaryNotFoundException(DIARY_NOT_FOUND, "일기를 찾을 수 없습니다."));

        // 기존 패턴과 일치하는 사용법
        if (!diary.getUser().getBifId().equals(bifId)) {
            throw new BifAccessForbiddenException(COMMON_FORBIDDEN, "일기 수정 권한이 없습니다.");
        }

        diary.setContent(content);
        diary.setUpdatedAt(LocalDateTime.now());
        
        return DiaryResponse.builder()
            .id(diary.getId())
            .content(diary.getContent())
            .userId(diary.getUser().getBifId())
            .emotion(diary.getEmotion())
            .aiFeedback(null)
            .createdAt(diary.getCreatedAt())
            .updatedAt(diary.getUpdatedAt())
            .build();
    }

    @Override
    @Transactional
    public void deleteDiary(Long bifId, Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new DiaryNotFoundException(DIARY_NOT_FOUND,"존재하지 않거나 이미 삭제된 일기입니다."));
        
        if(!diary.getUser().getBifId().equals(bifId)){
            throw new BifAccessForbiddenException(COMMON_FORBIDDEN, "일기 삭제 권한이 없습니다.");
        }

        diary.setDeleted(true);
    }

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