package com.sage.bif.diary.entity;

import com.sage.bif.diary.entity.converter.DiaryContentEncryptConverter;
import com.sage.bif.user.entity.Bif;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sage.bif.diary.model.Emotion;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "emotion_diary", indexes = {
        @Index(name="idx_bifid_createdAt",columnList = "bif_id, created_at")
})
@SQLRestriction("is_deleted = false")
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="diary_id")
    private Long id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, unique = true)
    private UUID uuid; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="bif_id", nullable = false)
    private Bif user;

    @Column(name="selected_emotion", nullable = false)
    private Emotion emotion;

    @Column(columnDefinition = "TEXT", nullable = false, length = 800)
    @Convert(converter = DiaryContentEncryptConverter.class)
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
    protected void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
        }
        if(this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
