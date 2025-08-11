package com.sage.bif.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "guardian")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Guardian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guardian_id")
    private Long guardianId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "social_id", nullable = false)
    private SocialLogin socialLogin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bif_id", nullable = false)
    private Bif bif;

    @Column(name = "nickname", nullable = false, unique = true, length = 20)
    private String nickname;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
