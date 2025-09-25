package com.sage.bif.diary.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
// import com.sage.bif.common.service.RedisService;
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
    // private final RedisService redisService;
    private final AiFeedbackService aiFeedbackService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public MonthlySummaryResponse getMonthlySummary(Long bifId, MonthlySummaryRequest request) {
        // Redis 캐시 로직을 주석 처리하고 직접 DB에서 조회
        /*
        MonthlySummaryResponse response=null;
        String cacheKey = String.format("monthly_summary:%d:%d:%d",
        bifId, request.getYear(), request.getMonth());

        Optional<Object> cached = redisService.get(cacheKey);

        if(cached.isPresent()){
            try {
                response = objectMapper.convertValue(cached.get(), MonthlySummaryResponse.class);
            } catch (Exception e) {
                log.warn("캐시된 월간 요약 데이터 변환 실패 -Key:{}, Error:{}", cacheKey, e.getMessage());
                redisService.delete(cacheKey);
                response = null;
            }
        }
        if(response == null){
            response = getMonthlySummaryFromDatabase(bifId, request);
            if(!response.getDailyEmotions().isEmpty()){
                try {
                    redisService.set(cacheKey, response, Duration.ofHours(1));
                    log.info("월간 요약 데이터 캐시 저장 완료: {}",cacheKey);
                } catch (Exception e) {
                    log.error("월간 요약 데이터 캐시 저장 실패 - Key:{}, Error:{}", cacheKey, e.getMessage());
                }
            }

        }
        */

        // DB에서 직접 조회
        MonthlySummaryResponse response = getMonthlySummaryFromDatabase(bifId, request);

        boolean canWriteToday = diaryRepository.existsByUserIdAndDate(bifId,LocalDate.now())==0;
        response.setCanWriteToday(canWriteToday);
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
                    .dailyEmotions(new HashMap<>())
                    .build();
        }
        Map<LocalDate, MonthlySummaryResponse.DailyInfo> dailyInfos = diaries.stream()
        .collect(Collectors.toMap(
            diary -> diary.getCreatedAt().toLocalDate(),
            diary -> MonthlySummaryResponse.DailyInfo.builder()
                .emotion(diary.getEmotion())
                .diaryId(diary.getUuid())
                .build()
        ));

        return MonthlySummaryResponse.builder()
                .year(request.getYear())
                .month(request.getMonth())
                .dailyEmotions(dailyInfos)
                .build();
    }

    @Override
    @Transactional
    public DiaryResponse getDiary(Long bifId, String uuid) {
        Diary diary = diaryRepository.findByUuidWithUser(UUID.fromString(uuid))
            .orElseThrow(() -> new DiaryNotFoundException(DIARY_NOT_FOUND, "존재하지 않거나 조회할 수 없는 일기입니다.\n일기를 다시 선택해주세요."));

        if (!diary.getUser().getBifId().equals(bifId)) {
            throw new BifAccessForbiddenException(COMMON_FORBIDDEN, "일기에 대한 접근 권한이 없습니다.\n다른 방식을 시도해주세요.");
        }
        
        AiFeedback feedback = diary.getAiFeedback();
        if(feedback == null){
            feedback = AiFeedback.builder()
            .diary(diary)
            .content(null)
            .build();

            aiFeedbackService.generateAiFeedbackAll(diary, feedback);
        }

        return DiaryResponse.builder()
                .id(diary.getUuid())
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

        LocalDate now = LocalDate.now();
        if(diaryRepository.existsByUserIdAndDate(bifId, now)>0){
            throw new DiaryAlreadyExistsException(now);
        }

        Diary diary = Diary.builder()
            .content(request.getContent())
            .user(user)
            .emotion(request.getEmotion())
            .isDeleted(false)
            .build();

        Diary savedDiary = diaryRepository.save(diary);
        
        AiFeedback feedback = aiFeedbackService.createAiFeedback(savedDiary);

        DiaryCreatedEvent event = new DiaryCreatedEvent(this, bifId, savedDiary.getContent(), savedDiary.getId());
        eventPublisher.publishEvent(event);

        return DiaryResponse.builder()
            .id(savedDiary.getUuid())
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
    public DiaryResponse updateDiaryContent(Long bifId, String uuid, String content) {
        Diary diary = diaryRepository.findByUuidWithUser(UUID.fromString(uuid))
            .orElseThrow(() -> new DiaryNotFoundException(DIARY_NOT_FOUND, "일기를 찾을 수 없습니다."));
        
        if (!diary.getUser().getBifId().equals(bifId)) {
            throw new BifAccessForbiddenException(COMMON_FORBIDDEN, "일기 수정 권한이 없습니다.");
        }

        String previousContent = diary.getContent();
        diary.setContent(content);
        diary.setUpdatedAt(LocalDateTime.now());

        AiFeedback feedback = diary.getAiFeedback();
        if (feedback == null) {
            feedback = AiFeedback.builder()
                    .diary(diary)
                    .build();
        }

        DiaryUpdatedEvent event = new DiaryUpdatedEvent(this, diary, previousContent);
        eventPublisher.publishEvent(event);

        return DiaryResponse.builder()
            .id(diary.getUuid())
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
    public void deleteDiary(Long bifId, String uuid) {
        Diary diary = diaryRepository.findByUuid(UUID.fromString(uuid))
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
