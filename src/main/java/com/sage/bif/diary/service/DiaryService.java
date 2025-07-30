package com.sage.bif.diary.service;

import com.sage.bif.diary.dto.request.DiaryCreateRequest;
import com.sage.bif.diary.dto.response.DiaryResponse;

public interface DiaryService {
    DiaryResponse createDiary(DiaryCreateRequest request);
} 