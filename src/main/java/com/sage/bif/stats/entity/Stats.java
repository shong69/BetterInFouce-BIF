package com.sage.bif.stats.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "emotion_statistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Stats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "statistics_id")
    private Long id;

    @Column(name = "bif_id", nullable = false)
    private Long bifId;

    @Column(name = "stats_year_month", nullable = false)
    private LocalDateTime yearMonth;

    @Column(name = "emotion_statistics_text", columnDefinition = "TEXT", nullable = false)
    private String emotionStatisticsText;

    @Column(name = "guardian_advice_text", columnDefinition = "TEXT", nullable = false)
    private String guardianAdviceText;

    @Column(name = "emotion_counts", columnDefinition = "TEXT")
    private String emotionCounts;

    @Column(name = "top_keywords", columnDefinition = "TEXT")
    private String topKeywords;

    @Column(name = "ai_emotion_score", columnDefinition = "DECIMAL(3,2)")
    private Double aiEmotionScore;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
