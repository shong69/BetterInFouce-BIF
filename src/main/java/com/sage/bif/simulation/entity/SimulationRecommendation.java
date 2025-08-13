package com.sage.bif.simulation.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import com.sage.bif.user.entity.Bif;
import com.sage.bif.user.entity.Guardian;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "simulation_recommendation")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class SimulationRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id")
    private Long recommendationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bif_id", nullable = false)
    private Bif bif;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id", nullable = false)
    private Guardian guardian;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_id", nullable = false)
    private Simulation simulation;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
