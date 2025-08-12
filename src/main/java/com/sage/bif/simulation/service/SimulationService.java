package com.sage.bif.simulation.service;

import com.sage.bif.simulation.dto.response.SimulationResponse;
import com.sage.bif.simulation.dto.response.SimulationChoiceResponse;

import com.sage.bif.simulation.dto.response.SimulationDetailsResponse;

import java.util.List;

public interface SimulationService {

    List<SimulationResponse> getAllSimulations();

    String startSimulation(Long simulationId);

    SimulationChoiceResponse submitChoice(String sessionId, String choice);

    SimulationDetailsResponse getSimulationDetails(Long simulationId);

    void recommendSimulation(Long simulationId);

    String getFeedbackText(Long simulationId, int score);

}
