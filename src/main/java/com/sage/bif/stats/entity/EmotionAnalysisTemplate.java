package com.sage.bif.stats.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "emotion_analysis_template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmotionAnalysisTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "okay_range", nullable = false)
    private String okayRange;

    @Column(name = "good_range", nullable = false)
    private String goodRange;

    @Column(name = "angry_range", nullable = false)
    private String angryRange;

    @Column(name = "down_range", nullable = false)
    private String downRange;

    @Column(name = "great_range", nullable = false)
    private String greatRange;

    @Column(name = "analysis_text", columnDefinition = "TEXT", nullable = false)
    private String analysisText;
} 
