package com.sage.bif.simulation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "simulation_step")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SimulationStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "step_id")
    private Long stepId;

    @Column(name = "simulation_id", nullable = false)
    private Long simulationId;

    @Column(name = "step_order", nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer stepOrder;

    @Column(name = "character_line", nullable = false, columnDefinition = "TEXT")
    private String characterLine;

}
