package com.sage.bif.diary.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
import com.sage.bif.common.service.RedisService;
import com.sage.bif.diary.dto.request.DiaryRequest;
import com.sage.bif.diary.dto.request.MonthlySummaryRequest;
import com.sage.bif.diary.dto.response.DiaryResponse;
import com.sage.bif.diary.dto.response.MonthlySummaryResponse;
import com.sage.bif.diary.entity.AiFeedback;
import com.sage.bif.diary.entity.Diary;
import com.sage.bif.diary.exception.AiServiceException;
import com.sage.bif.diary.exception.DiaryNotFoundException;
import com.sage.bif.diary.exception.BifAccessForbiddenException;
import com.sage.bif.diary.exception.DiaryAlreadyExistsException;
import com.sage.bif.diary.repository.AiFeedbackRepository;
import com.sage.bif.diary.repository.DiaryRepository;
import com.sage.bif.user.entity.Bif;
import com.sage.bif.user.repository.BifRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.sage.bif.diary.event.model.DiaryCreatedEvent;
import com.sage.bif.diary.event.model.DiaryUpdatedEvent;
import com.sage.bif.diary.event.model.DiaryDeletedEvent;

import static com.sage.bif.common.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {
    
    private final DiaryRepository diaryRepository;
    private final AiFeedbackRepository aiFeedbackRepository;
    private final BifRepository bifRepository;
    private final ObjectMapper objectMapper;
    private final RedisService redisService;
    private final AiFeedbackService aiFeedbackService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public MonthlySummaryResponse getMonthlySummary(Long bifId, MonthlySummaryRequest request) {

        String cacheKey = String.format("monthly_summary:%d:%d:%d", 
        bifId, request.getYear(), request.getMonth());

        Optional<Object> cached = redisService.get(cacheKey);

        if(cached.isPresent()){
            try {
                MonthlySummaryResponse response = objectMapper.convertValue(cached.get(), MonthlySummaryResponse.class);
                
                if (response != null && 
                    response.getYear() == request.getYear() && 
                    response.getMonth() == request.getMonth() &&
                    response.getDailyEmotions() != null) {
                    return response;
                } else {
                    log.warn("캐시된 데이터가 유효하지 않음 - Key: {}, Response: {}, dailyEmotions: {}",
                            cacheKey, response, response != null ? response.getDailyEmotions() : "null");
                    redisService.delete(cacheKey);
                }
            } catch (Exception e) {
                log.error("캐시된 월간 요약 데이터 변환 실패 - Key: {}, Error: {}", cacheKey, e.getMessage());
                redisService.delete(cacheKey);
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

        LocalDate today = LocalDate.now();
        boolean canWriteToday = diaryRepository.existsByUserIdAndDate(bifId, today)==0;

        if(diaries.isEmpty()){
            return MonthlySummaryResponse.builder()
                    .year(request.getYear())
                    .month(request.getMonth())
                    .dailyEmotions(new HashMap<>())
                    .canWriteToday(canWriteToday)
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
                .canWriteToday(canWriteToday)
                .build();
    }

    @Override
    @Transactional
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
        boolean exists = diaryRepository.existsByUserIdAndDate(bifId, request.getDate().toLocalDate())>0;
        
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

        AiFeedback feedback = null;
        
        log.info("checkModeration 호출 시작 - content: {}", request.getContent());
        
        try {
            AiFeedback tempFeedback = AiFeedback.builder()
                .diary(savedDiary)
                .content("")
                .build();
            
            aiFeedbackService.checkModeration(request.getContent(), tempFeedback, bifId, savedDiary.getId());
            log.info("checkModeration 호출 완료 - flagged: {}", tempFeedback.isContentFlagged());
            
            if (!tempFeedback.isContentFlagged()) {
                String aiFeedbackContent = aiFeedbackService.generateAiFeedback(request.getContent(), request.getEmotion());
                if (aiFeedbackContent != null && !aiFeedbackContent.trim().isEmpty()) {
                    feedback = AiFeedback.builder()
                        .diary(savedDiary)
                        .content(aiFeedbackContent)
                        .contentFlagged(false)
                        .contentFlaggedCategories(null)
                        .build();
                    
                    feedback = aiFeedbackRepository.save(feedback);
                    log.info("AI 피드백 저장 완료 - ID: {}", feedback.getId());
                } else {
                    log.warn("AI 피드백 생성 실패 또는 빈 내용 - content: {}", aiFeedbackContent);
                }
            } else {
                feedback = AiFeedback.builder()
                    .diary(savedDiary)
                    .content(null)
                    .contentFlagged(true)
                    .contentFlaggedCategories(tempFeedback.getContentFlaggedCategories())
                    .build();
                 
                feedback = aiFeedbackRepository.save(feedback);
                log.info("Moderation 결과만 저장 - flagged: true, categories: {}", 
                    tempFeedback.getContentFlaggedCategories());
             }
            
        } catch (Exception e) {
            log.error("AI 서비스 호출 중 예외 발생: {}", e.getMessage(), e);
        }

        DiaryCreatedEvent event = new DiaryCreatedEvent(this, savedDiary);
        eventPublisher.publishEvent(event);
        
        return DiaryResponse.builder()
            .id(savedDiary.getId())
            .content(savedDiary.getContent())
            .userId(bifId)
            .emotion(savedDiary.getEmotion())
            .aiFeedback(Optional.ofNullable(feedback).map(AiFeedback::getContent).orElse(null))
            .contentFlagged(Optional.ofNullable(feedback).map(AiFeedback::isContentFlagged).orElse(false))
            .contentFlaggedCategories(Optional.ofNullable(feedback).map(AiFeedback::getContentFlaggedCategories).orElse(null))
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

        String previousContent = diary.getContent();
        diary.setContent(content);
        diary.setUpdatedAt(LocalDateTime.now());

        Optional<AiFeedback> existingFeedback = aiFeedbackRepository.findByDiary(diary);
        if (existingFeedback.isPresent()) {
            AiFeedback feedback = existingFeedback.get();
            aiFeedbackService.checkModeration(content, feedback, bifId, diaryId);
            aiFeedbackRepository.save(feedback);
        }

        DiaryUpdatedEvent event = new DiaryUpdatedEvent(this, diary, previousContent);
        eventPublisher.publishEvent(event);

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

        Long deletedDiaryId = diary.getId();
        String deletedContent = diary.getContent();
        String emotion = diary.getEmotion().toString();

        diary.setDeleted(true);

        DiaryDeletedEvent event = new DiaryDeletedEvent(this, deletedDiaryId, bifId, deletedContent, emotion);
        eventPublisher.publishEvent(event);
    }

}
