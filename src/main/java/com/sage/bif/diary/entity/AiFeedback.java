package com.sage.bif.diary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name="emotion_feedback")
public class AiFeedback {

    @Id
    @Column(name="feedback_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="diary_id", nullable = true, unique = true)
    private Diary diary;

    @Column(columnDefinition = "TEXT", nullable=true)
    private String content;

    @Column(name="content_flagged", nullable=false)
    @Builder.Default
    private boolean contentFlagged=false;

    @Column(name="content_flagged_categories", columnDefinition = "TEXT")
    private String contentFlaggedCategories;

    @Column(name="created_at", columnDefinition = "TIMESTAMP", nullable=false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
