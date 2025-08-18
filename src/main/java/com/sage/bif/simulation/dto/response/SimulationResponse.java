package com.sage.bif.simulation.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.sage.bif.simulation.entity.Simulation;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SimulationResponse from(Simulation simulation, Boolean isActive) {

        return SimulationResponse.builder()
                .id(simulation.getId())
                .title(simulation.getTitle())
                .description(simulation.getDescription())
                .category(simulation.getCategory())
                .createdAt(simulation.getCreatedAt())
                .updatedAt(simulation.getUpdatedAt())
                .isActive(isActive)
                .build();
    }

    public static SimulationResponse from(Simulation simulation) {
        return from(simulation, false);
    }

}
