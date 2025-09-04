package com.sage.bif.simulation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationRecommendationRequest {
    
    @NotNull(message = "시뮬레이션 ID는 필수입니다.")
    private Long simulationId;
    
    @NotNull(message = "BIF ID는 필수입니다.")
    private Long bifId;

}
