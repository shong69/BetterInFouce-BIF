package com.sage.bif.diary.service;

import com.sage.bif.diary.dto.request.DiaryCreateRequest;
import com.sage.bif.diary.dto.request.MonthlySummaryRequest;
import com.sage.bif.diary.dto.response.DiaryResponse;
import com.sage.bif.diary.dto.response.MonthlySummaryResponse;

public interface DiaryService {
    MonthlySummaryResponse getMonthlySummary(MonthlySummaryRequest request);
//    DiaryResponse getDiary(Long diaryId);
    DiaryResponse createDiary(DiaryCreateRequest request);
    DiaryResponse updateDiaryContent(Long diaryId, String content);
    void deleteDiary(Long diaryId);
}