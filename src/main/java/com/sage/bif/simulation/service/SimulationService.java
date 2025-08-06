package com.sage.bif.simulation.service;

import com.sage.bif.simulation.dto.response.SimulationResponse;
import com.sage.bif.simulation.dto.response.SimulationSessionResponse;
import com.sage.bif.simulation.dto.response.SimulationChoiceResponse;
import com.sage.bif.simulation.dto.response.SimulationResultResponse;
import com.sage.bif.simulation.dto.response.SimulationDetailsResponse;
import com.sage.bif.simulation.dto.request.SimulationChoiceRequest;
import java.util.List;

public interface SimulationService {
    
    List<SimulationResponse> getAllSimulations();
    
    SimulationSessionResponse startSimulation(Long simulationId);
    
    SimulationChoiceResponse submitChoice(String sessionId, SimulationChoiceRequest request);
    
    SimulationResultResponse getSimulationResult(String sessionId);
    
    SimulationDetailsResponse getSimulationDetails(Long simulationId);
} 