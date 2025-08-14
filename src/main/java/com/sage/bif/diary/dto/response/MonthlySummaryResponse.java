package com.sage.bif.diary.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.sage.bif.diary.model.Emotion;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class MonthlySummaryResponse {

    private int year;
    private int month;
    private Map<LocalDate, DailyInfo> dailyEmotions;
    private boolean canWriteToday;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class DailyInfo {
        private Emotion emotion;
        private UUID diaryId;
    }
}
