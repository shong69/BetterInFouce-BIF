package com.sage.bif.simulation.service;

import com.sage.bif.simulation.dto.response.SimulationResponse;
import com.sage.bif.simulation.dto.response.SimulationChoiceResponse;
import com.sage.bif.simulation.dto.response.SimulationDetailsResponse;
import com.sage.bif.simulation.dto.response.SimulationRecommendationResponse;

import java.util.List;
import java.util.Map;

public interface SimulationService {

    List<SimulationResponse> getAllSimulations(Long guardianId, Long bifId);

    String startSimulation(Long simulationId);

    SimulationChoiceResponse submitChoice(String sessionId, String choice);

    SimulationDetailsResponse getSimulationDetails(Long simulationId);

    SimulationRecommendationResponse clickRecommendation(Long guardianId, Long bifId, Long simulationId);

    String getFeedbackText(Long simulationId, int score);

    Map<String, Object> convertTextToSpeech(String text, String voiceName) throws Exception;

}
