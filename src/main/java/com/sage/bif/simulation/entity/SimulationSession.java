package com.sage.bif.simulation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "simulation_session")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class SimulationSession {
    
    @Id
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    
    @Column(name = "simulation_id", nullable = false)
    private Long simulationId;
    
    @Column(name = "current_step", nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer currentStep;
    
    @Column(name = "total_score", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer totalScore;
    
    @Column(name = "is_completed", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isCompleted;
}

