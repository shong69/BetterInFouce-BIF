package com.sage.bif.simulation.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationDetailsResponse {

    private Long simulationId;
    private String simulationTitle;
    private String description;
    private String category;
    private int totalSteps;
    private List<SimulationStepResponse> steps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimulationStepResponse {
        private int stepNumber;
        private String scenarioText;
        private List<SimulationChoiceOptionResponse> choices;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimulationChoiceOptionResponse {
        private String choiceText;
        private int choiceScore;
        private String feedbackText;
    }

}
