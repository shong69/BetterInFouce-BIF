package com.sage.bif.stats.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Stats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_id")
    private Long id;

    @Column(name = "bif_id", nullable = false)
    private Long bifId;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "emotion_counts", columnDefinition = "JSON", nullable = false)
    private String emotionCounts;

    @Column(name = "top_keywords", columnDefinition = "JSON")
    private String topKeywords;

    @Column(name = "emotion_analysis_text", columnDefinition = "TEXT", nullable = false)
    private String emotionAnalysisText;

    @Column(name = "is_current_month", nullable = false)
    @Builder.Default
    private Boolean isCurrentMonth = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
} 