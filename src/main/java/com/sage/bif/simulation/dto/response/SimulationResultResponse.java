package com.sage.bif.simulation.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResultResponse {
    private String sessionId;
    private String finalResult;
    private List<String> choices;
    private int score;
    private int percentage;
    private String feedback;
} 