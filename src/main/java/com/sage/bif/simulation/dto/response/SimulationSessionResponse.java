package com.sage.bif.simulation.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationSessionResponse {
    
    private String sessionId;
    private Long simulationId;
    private String simulationTitle;
    private String category;
    private String currentStep;
    private String scenario;
    private String[] choices;
    private Boolean isCompleted;
}