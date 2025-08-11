package com.sage.bif.diary.entity;

import com.sage.bif.user.entity.Bif;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.sage.bif.diary.model.Emotion;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "Emotion_Diary", indexes = {
        @Index(name="idx_bifid_createdAt",columnList = "bif_id, created_at")
})
@SQLRestriction("is_deleted = false")
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
    
    @Column(columnDefinition = "TEXT", nullable = false, length = 800)
    private String content;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name="is_deleted", nullable=false)
    @Builder.Default
    private boolean isDeleted=false;

    @OneToOne(mappedBy = "diary", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AiFeedback aiFeedback;

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
