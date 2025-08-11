package com.sage.bif.diary.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sage.bif.common.client.ai.AiServiceClient;
import com.sage.bif.common.client.ai.AzureOpenAiModerationClient;
import com.sage.bif.common.client.ai.dto.AiRequest;
import com.sage.bif.common.client.ai.dto.AiResponse;
import com.sage.bif.common.client.ai.dto.ModerationResponse;
import com.sage.bif.common.client.ai.AiSettings;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
import com.sage.bif.common.service.RedisService;
import com.sage.bif.diary.dto.request.DiaryRequest;
import com.sage.bif.diary.dto.request.MonthlySummaryRequest;
import com.sage.bif.diary.dto.response.DiaryResponse;
import com.sage.bif.diary.dto.response.MonthlySummaryResponse;
import com.sage.bif.diary.entity.AiFeedback;
import com.sage.bif.diary.entity.Diary;
import com.sage.bif.diary.exception.DiaryNotFoundException;
import com.sage.bif.diary.exception.BifAccessForbiddenException;
import com.sage.bif.diary.exception.DiaryAlreadyExistsException;
import com.sage.bif.diary.repository.AiFeedbackRepository;
import com.sage.bif.diary.repository.DiaryRepository;
import com.sage.bif.user.entity.Bif;
import com.sage.bif.user.repository.BifRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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

    private final ObjectMapper objectMapper;
    private final RedisService redisService;
    private final AsyncService asyncService;
    private final AiFeedbackService aiFeedbackService;

    @Override
    public MonthlySummaryResponse getMonthlySummary(Long bifId, MonthlySummaryRequest request) {

        String cacheKey = String.format("monthly_summary:%d:%d:%d", 
        bifId, request.getYear(), request.getMonth());

        Optional<Object> cached = redisService.get(cacheKey);

        if(cached.isPresent()){
            try {
                MonthlySummaryResponse response = objectMapper.convertValue(cached.get(), MonthlySummaryResponse.class);
                log.info("캐시에서 월간 요약 조회 : {}", cacheKey);
                return response;
            } catch (Exception e) {
                log.error("캐시된 월간 요약 데이터 변환 실패 - Key: {}, Error: {}", cacheKey, e.getMessage());
            }
        }
        MonthlySummaryResponse response = getMonthlySummaryFromDatabase(bifId, request);
        try {
            redisService.set(cacheKey, response, Duration.ofHours(1));
            log.info("월간 요약 데이터 캐시 저장 완료 : {}", cacheKey);
        } catch (Exception e) {
            log.error("월간 요약 데이터 캐시 저장 실패 - Key: {}, Error: {}", cacheKey, e.getMessage());
        }
        return response;
    }

    private MonthlySummaryResponse getMonthlySummaryFromDatabase(Long bifId, MonthlySummaryRequest request) {
        LocalDate startDate = LocalDate.of(request.getYear(), request.getMonth(),1);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = start.plusMonths(1);

        List<Diary> diaries = diaryRepository.findByUserIdAndDateBetween(bifId,start,end);
        if(diaries.isEmpty()){
            return MonthlySummaryResponse.builder()
                    .year(request.getYear())
                    .month(request.getMonth())
                    .build();
        }
        Map<LocalDate, MonthlySummaryResponse.DailyInfo> dailyInfos = diaries.stream()
        .collect(Collectors.toMap(
            diary -> diary.getCreatedAt().toLocalDate(),
            diary -> MonthlySummaryResponse.DailyInfo.builder()
                .emotion(diary.getEmotion())
                .diaryId(diary.getId())
                .build()
        ));

        return MonthlySummaryResponse.builder()
                .year(request.getYear())
                .month(request.getMonth())
                .dailyEmotions(dailyInfos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DiaryResponse getDiary(Long bifId, Long diaryId) {
        Diary diary = diaryRepository.findByIdWithUser(diaryId)
            .orElseThrow(() -> new DiaryNotFoundException(DIARY_NOT_FOUND, "일기를 찾을 수 없습니다."));

        if (!diary.getUser().getBifId().equals(bifId)) {
            throw new BifAccessForbiddenException(COMMON_FORBIDDEN, "일기에 대한 접근 권한이 없습니다.");
        }
        
        AiFeedback feedback = diary.getAiFeedback();
        if(feedback == null){
            feedback = AiFeedback.builder()
            .diary(diary)
            .content(null)
            .build();

            aiFeedbackService.regenerateAiFeedbackIfNeeded(diary, feedback);
        }

        return DiaryResponse.builder()
                .id(diary.getId())
                .content(diary.getContent())
                .userId(diary.getUser().getBifId())
                .emotion(diary.getEmotion())
                .aiFeedback(feedback.getContent())
                .contentFlagged(feedback.isContentFlagged())
                .contentFlaggedCategories(feedback.getContentFlaggedCategories())
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
        .content(null) 
        .build();
    
        aiFeedbackService.checkModeration(request.getContent(), feedback, bifId, savedDiary.getId());

        if (!feedback.isContentFlagged()) {
            String aiFeedbackContent = aiFeedbackService.generateAiFeedback(request.getContent());
            feedback.setContent(aiFeedbackContent);
        }

        aiFeedbackRepository.save(feedback);

        asyncService.invalidateCacheAsync(bifId, request.getDate().toLocalDate());//비동기
        
        return DiaryResponse.builder()
            .id(savedDiary.getId())
            .content(savedDiary.getContent())
            .userId(bifId)
            .emotion(savedDiary.getEmotion())
            .aiFeedback(feedback.getContent())
            .contentFlagged(feedback.isContentFlagged())
            .contentFlaggedCategories(feedback.getContentFlaggedCategories())
            .createdAt(savedDiary.getCreatedAt())
            .updatedAt(savedDiary.getUpdatedAt())
            .build();
    }

    @Override
    @Transactional
    public DiaryResponse updateDiaryContent(Long bifId, Long diaryId, String content) {
        Diary diary = diaryRepository.findByIdWithUser(diaryId)
            .orElseThrow(() -> new DiaryNotFoundException(DIARY_NOT_FOUND, "일기를 찾을 수 없습니다."));
        
        if (!diary.getUser().getBifId().equals(bifId)) {
            throw new BifAccessForbiddenException(COMMON_FORBIDDEN, "일기 수정 권한이 없습니다.");
        }

        diary.setContent(content);
        diary.setUpdatedAt(LocalDateTime.now());

        Optional<AiFeedback> existingFeedback = aiFeedbackRepository.findByDiary(diary);
        if (existingFeedback.isPresent()) {
            AiFeedback feedback = existingFeedback.get();
            aiFeedbackService.checkModeration(content, feedback, bifId, diaryId);
            aiFeedbackRepository.save(feedback);
        }

        asyncService.invalidateCacheAsync(bifId, diary.getCreatedAt().toLocalDate());

        return DiaryResponse.builder()
            .id(diary.getId())
            .content(diary.getContent())
            .userId(diary.getUser().getBifId())
            .emotion(diary.getEmotion())
            .aiFeedback(existingFeedback.map(AiFeedback::getContent).orElse(null))
            .contentFlagged(existingFeedback.map(AiFeedback::isContentFlagged).orElse(false))
            .contentFlaggedCategories(existingFeedback.map(AiFeedback::getContentFlaggedCategories).orElse(null))
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

        asyncService.invalidateCacheAsync(bifId, diary.getCreatedAt().toLocalDate());
    }
} 