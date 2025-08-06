package com.sage.bif.simulation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "simulation_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class SimulationSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", unique = true, nullable = false)
    private String sessionId;
    
    @Column(name = "simulation_id", nullable = false)
    private Long simulationId;
    
    @Column(name = "current_step", nullable = false)
    private Integer currentStep;
    
    @Column(name = "total_score", nullable = false)
    private Integer totalScore;
    
    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted;
}

