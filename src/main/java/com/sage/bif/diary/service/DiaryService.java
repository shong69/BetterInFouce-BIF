package com.sage.bif.diary.service;

import com.sage.bif.diary.dto.request.DiaryRequest;
import com.sage.bif.diary.dto.request.MonthlySummaryRequest;
import com.sage.bif.diary.dto.response.DiaryResponse;
import com.sage.bif.diary.dto.response.MonthlySummaryResponse;

public interface DiaryService {
    MonthlySummaryResponse getMonthlySummary(Long bifId, MonthlySummaryRequest request);
    DiaryResponse getDiary(Long bifId, Long diaryId);
    DiaryResponse createDiary(Long bifId, DiaryRequest request);
    DiaryResponse updateDiaryContent(Long bifId, Long diaryId, String content);
    void deleteDiary(Long bifId, Long diaryId);
}