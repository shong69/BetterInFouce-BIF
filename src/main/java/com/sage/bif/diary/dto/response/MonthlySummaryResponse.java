package com.sage.bif.diary.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.sage.bif.diary.model.Emotion;

@Getter
@Setter
@Builder
public class MonthlySummaryResponse {
    private int year;
    private int month;
    private Map<LocalDate, Emotion> dailyEmotions;
    private List<DiarySummary> diarySummaries;

    @Getter
    @Setter
    @Builder
    public static class DiarySummary {
        private Long id;
        private LocalDate date;
        private Emotion emotion;
    }

} 