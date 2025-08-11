package com.sage.bif.diary.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.Map;

import com.sage.bif.diary.model.Emotion;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlySummaryResponse {
    private int year;
    private int month;
    private Map<LocalDate, DailyInfo> dailyEmotions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyInfo {
        private Emotion emotion;
        private Long diaryId;
    }
}
