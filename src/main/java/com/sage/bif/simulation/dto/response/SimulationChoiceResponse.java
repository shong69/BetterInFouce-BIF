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
public class SimulationChoiceResponse {
    
    private String sessionId;
    private String selectedChoice;
    private String feedback;
    private String nextScenario;
    private String[] nextChoices;
    private Integer currentScore;
    private Boolean isCompleted;

}
