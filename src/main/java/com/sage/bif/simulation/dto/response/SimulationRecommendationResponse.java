package com.sage.bif.simulation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationRecommendationResponse {

    private List<Long> recommendedSimulationIds;

    private Boolean isActive;

}
