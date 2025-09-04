package com.sage.bif.simulation.entity;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bif_choice")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class BifChoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "choice_id")
    private Long choiceId;

    @Column(name = "step_id", nullable = false)
    private Long stepId;

    @Column(name = "simulation_id", nullable = false)
    private Long simulationId;

    @Column(name = "choice_text", nullable = false, columnDefinition = "TEXT")
    private String choiceText;

    @Column(name = "choice_score", nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer choiceScore;

    @Column(name = "feedback_text", columnDefinition = "TEXT")
    private String feedbackText;

}
