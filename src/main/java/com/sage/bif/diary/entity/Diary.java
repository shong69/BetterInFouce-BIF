package com.sage.bif.diary.entity;

import com.sage.bif.user.entity.Bif;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.sage.bif.diary.model.Emotion;

@Entity
@Table(name = "Emotion_Diary", indexes = {
        @Index(name="idx_bifid_createdAt",columnList = "bif_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Diary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="diary_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="bif_id", nullable = false)
    private Bif user;

    @Column(name="selected_emotion", nullable = false)
    private Emotion emotion;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name="is_deleted", nullable=false)
    private boolean isDeleted;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 