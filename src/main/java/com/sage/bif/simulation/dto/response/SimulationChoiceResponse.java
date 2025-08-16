package com.sage.bif.simulation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Integer choiceScore;
    private Integer totalScore;
    private Boolean isCompleted;

}
