package com.sage.bif.simulation.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SimulationChoiceRequest {
    private String choice;
    private String sessionId;
} 