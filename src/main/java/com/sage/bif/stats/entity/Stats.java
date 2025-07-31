package com.sage.bif.stats.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "emotion_analysis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Stats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long id;

    @Column(name = "bif_id", nullable = false)
    private Long bifId;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month_value", nullable = false)
    private Integer month;

    @Column(name = "emotion_analysis_text", columnDefinition = "TEXT", nullable = false)
    private String emotionAnalysisText;

    @Column(name = "guardian_advice_text", columnDefinition = "TEXT", nullable = false)
    private String guardianAdviceText;

    @Column(name = "emotion_counts", columnDefinition = "JSON")
    private String emotionCounts;

    @Column(name = "top_keywords", columnDefinition = "JSON")
    private String topKeywords;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
} 
