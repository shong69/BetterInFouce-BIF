package com.sage.bif.simulation.service;

import com.sage.bif.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import java.util.*;

import com.sage.bif.simulation.entity.Simulation;
import com.sage.bif.simulation.entity.SimulationStep;
import com.sage.bif.simulation.entity.BifChoice;
import com.sage.bif.simulation.entity.SimulationFeedback;
import com.sage.bif.simulation.repository.SimulationRepository;
import com.sage.bif.simulation.repository.SimulationStepRepository;
import com.sage.bif.simulation.repository.BifChoiceRepository;
import com.sage.bif.simulation.repository.SimulationFeedbackRepository;
import com.sage.bif.simulation.repository.SimulationRecommendationRepository;
import com.sage.bif.simulation.entity.SimulationRecommendation;
import com.sage.bif.simulation.dto.response.SimulationResponse;
import com.sage.bif.simulation.dto.response.SimulationChoiceResponse;
import com.sage.bif.simulation.dto.response.SimulationDetailsResponse;
import com.sage.bif.simulation.dto.response.SimulationRecommendationResponse;
import com.sage.bif.user.entity.Bif;
import com.sage.bif.user.entity.Guardian;
import com.sage.bif.user.repository.BifRepository;
import com.sage.bif.user.repository.GuardianRepository;

import com.sage.bif.simulation.exception.SimulationException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class SimulationServiceImpl implements SimulationService {

    private final SimulationRepository simulationRepository;
    private final SimulationStepRepository stepRepository;
    private final BifChoiceRepository choiceRepository;
    private final SimulationFeedbackRepository feedbackRepository;
    private final SimulationRecommendationRepository recommendationRepository;
    private final GuardianRepository guardianRepository;
    private final BifRepository bifRepository;
    @Value("${GOOGLE_TTS_API_KEY}")
    private String googleTtsApiKey;


    @Override
    @Transactional(readOnly = true)
    public List<SimulationResponse> getAllSimulations(Long guardianId, Long bifId) {
        List<Object[]> results;

        results = simulationRepository.findAllSimulationsWithRecommendationStatus(guardianId, bifId);

        return results.stream()
                .map(result -> {
                    Simulation simulation = (Simulation) result[0];
                    Boolean isActive = (Boolean) result[1];
                    return SimulationResponse.from(simulation, isActive != null && isActive);
                })
                .toList();
    }

    @Override
    public String startSimulation(Long simulationId) {
        simulationRepository.findById(simulationId)
                .orElseThrow(() -> new SimulationException(ErrorCode.SIM_NOT_FOUND));

        return "simrun_" + UUID.randomUUID() + "_" + simulationId + "_1";
    }

    @Override
    public SimulationChoiceResponse submitChoice(String sessionId, String userChoice) {

        Long simulationId = extractSimulationIdFromSessionId(sessionId);
        int currentStep = extractCurrentStepFromSessionId(sessionId);

        SimulationStep currentStepData = stepRepository.findBySimulationIdAndStepOrder(simulationId, currentStep)
                .orElseThrow(() -> new SimulationException(ErrorCode.SIM_NOT_FOUND));

        List<BifChoice> choices = choiceRepository.findByStepIdOrderByChoiceId(currentStepData.getStepId());

        BifChoice selectedChoice = choices.stream()
                .findFirst()
                .orElseThrow(() -> new SimulationException(ErrorCode.SIM_INVALID_CHOICE));

        int currentChoiceScore = selectedChoice.getChoiceScore();
        int totalScore = calculateTotalScore(currentChoiceScore);

        int nextStep = currentStep + 1;
        List<SimulationStep> allSteps = stepRepository.findBySimulationIdOrderByStepOrder(simulationId);
        boolean isCompleted = nextStep > allSteps.size();

        String nextScenario = "";
        String[] nextChoices = new String[0];

        if (!isCompleted) {
            SimulationStep nextStepData = stepRepository.findBySimulationIdAndStepOrder(simulationId, nextStep)
                    .orElseThrow(() -> new SimulationException(ErrorCode.SIM_NOT_FOUND));

            List<BifChoice> nextStepChoices = choiceRepository.findByStepIdOrderByChoiceId(nextStepData.getStepId());
            nextScenario = nextStepData.getCharacterLine();
            nextChoices = nextStepChoices.stream()
                    .map(BifChoice::getChoiceText)
                    .toArray(String[]::new);
        }

        String nextSessionId = "session_" + System.currentTimeMillis() + "_" + simulationId + "_" + nextStep;

        return SimulationChoiceResponse.builder()
                .sessionId(nextSessionId)
                .selectedChoice(userChoice)
                .feedback(selectedChoice.getFeedbackText() != null ? selectedChoice.getFeedbackText() : "")
                .nextScenario(nextScenario)
                .nextChoices(nextChoices)
                .currentScore(currentChoiceScore)
                .choiceScore(currentChoiceScore)
                .totalScore(totalScore)
                .isCompleted(isCompleted)
                .build();
    }

    @Override
    public String getFeedbackText(Long simulationId, int score) {

        List<SimulationFeedback> allFeedbacks = feedbackRepository.findBySimulationId(simulationId);

        Optional<SimulationFeedback> feedback = feedbackRepository.findBySimulationIdAndScore(simulationId, score);

        if (feedback.isPresent()) {
            return feedback.get().getFeedbackText();
        }

        Optional<SimulationFeedback> closestFeedback = allFeedbacks.stream()
                .filter(f -> score >= f.getMinScore() && score <= f.getMaxScore())
                .findFirst();

        if (closestFeedback.isPresent()) {
            return closestFeedback.get().getFeedbackText();
        }

        return "피드백을 찾을 수 없습니다. 점수: " + score;
    }

    @Override
    public SimulationDetailsResponse getSimulationDetails(Long simulationId) {

        Simulation simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new SimulationException(ErrorCode.SIM_NOT_FOUND));

        List<SimulationStep> steps = stepRepository.findBySimulationIdOrderByStepOrder(simulationId);

        List<SimulationDetailsResponse.SimulationStepResponse> stepResponses = steps.stream()
                .map(step -> {
                    List<BifChoice> choices = choiceRepository.findByStepIdOrderByChoiceId(step.getStepId());

                    List<SimulationDetailsResponse.SimulationChoiceOptionResponse> choiceOptions = choices.stream()
                            .map(choice -> SimulationDetailsResponse.SimulationChoiceOptionResponse.builder()
                                    .choiceText(choice.getChoiceText())
                                    .choiceScore(choice.getChoiceScore())
                                    .feedbackText(choice.getFeedbackText() != null ? choice.getFeedbackText() : "")
                                    .build())
                            .toList();

                    return SimulationDetailsResponse.SimulationStepResponse.builder()
                            .stepNumber(step.getStepOrder())
                            .scenarioText(step.getCharacterLine())
                            .choices(choiceOptions)
                            .build();
                })
                .toList();

        return SimulationDetailsResponse.builder()
                .simulationId(simulationId)
                .simulationTitle(simulation.getTitle())
                .description(simulation.getDescription())
                .category(simulation.getCategory())
                .totalSteps(steps.size())
                .steps(stepResponses)
                .build();
    }

    @Override
    public SimulationRecommendationResponse clickRecommendation(Long guardianId, Long bifId, Long simulationId) {

        Guardian guardian = guardianRepository.findById(guardianId)
                .orElseThrow(() -> new SimulationException(ErrorCode.USER_NOT_FOUND));

        Bif bif = bifRepository.findById(bifId)
                .orElseThrow(() -> new SimulationException(ErrorCode.USER_NOT_FOUND));

        Simulation simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new SimulationException(ErrorCode.SIM_NOT_FOUND));

        Optional<SimulationRecommendation> existingRecommendation = recommendationRepository
                .findByGuardianGuardianIdAndBifBifIdAndSimulationId(guardianId, bifId, simulationId);

        Boolean isActive;

        if (existingRecommendation.isPresent()) {
            SimulationRecommendation recommendation = existingRecommendation.get();
            recommendation.setIsActive(!recommendation.getIsActive());
            recommendationRepository.save(recommendation);
            isActive = recommendation.getIsActive();
        } else {
            SimulationRecommendation newRecommendation = new SimulationRecommendation();
            newRecommendation.setGuardian(guardian);
            newRecommendation.setBif(bif);
            newRecommendation.setSimulation(simulation);
            newRecommendation.setIsActive(true);

            recommendationRepository.save(newRecommendation);
            isActive = true;
        }

        return SimulationRecommendationResponse.builder()
                .isActive(isActive)
                .build();
    }

    private Long extractSimulationIdFromSessionId(String sessionId) {

        String[] parts = sessionId.split("_");
        if (parts.length >= 4) {
            return Long.parseLong(parts[2]);
        }
        throw new IllegalArgumentException("잘못된 sessionId 형식: " + sessionId);
    }

    private int extractCurrentStepFromSessionId(String sessionId) {

        String[] parts = sessionId.split("_");
        if (parts.length >= 4) {
            return Integer.parseInt(parts[3]);
        }
        throw new IllegalArgumentException("잘못된 sessionId 형식: " + sessionId);
    }

    private int calculateTotalScore(int currentChoiceScore) {

        return currentChoiceScore;
    }

    @Override
    public Map<String, Object> convertTextToSpeech(String text, String voiceName) {

        validateTextInput(text);

        try {
            Map<String, Object> requestData = buildTtsRequest(text, voiceName);
            ResponseEntity<Map<String, Object>> response = callGoogleTtsApi(requestData);
            return processApiResponse(response);
        } catch (SimulationException e) {
            throw e;
        } catch (Exception e) {
            throw new SimulationException(ErrorCode.SIM_TTS_API_CALL_FAILED, e);
        }
    }

    private void validateTextInput(String text) {

        if (text == null || text.trim().isEmpty()) {
            throw new SimulationException(ErrorCode.SIM_TTS_INVALID_REQUEST, "변환할 텍스트가 없습니다.");
        }

        if (text.length() > 5000) {
            throw new SimulationException(ErrorCode.SIM_TTS_TEXT_TOO_LONG);
        }
    }

    private Map<String, Object> buildTtsRequest(String text, String voiceName) {

        String selectedVoice = (voiceName != null) ? voiceName : "ko-KR-Neural2-A";

        Map<String, Object> requestData = new HashMap<>();

        Map<String, String> input = new HashMap<>();
        input.put("text", text);
        requestData.put("input", input);

        Map<String, String> voice = new HashMap<>();
        voice.put("languageCode", "ko-KR");
        voice.put("name", selectedVoice);
        voice.put("ssmlGender", selectedVoice.contains("B") || selectedVoice.contains("Charon") ? "MALE" : "FEMALE");
        requestData.put("voice", voice);

        Map<String, Object> audioConfig = new HashMap<>();
        audioConfig.put("audioEncoding", "MP3");
        audioConfig.put("speakingRate", 1.1);
        audioConfig.put("pitch", 0.0);
        audioConfig.put("volumeGainDb", 0.0);
        requestData.put("audioConfig", audioConfig);

        return requestData;
    }

    private ResponseEntity<Map<String, Object>> callGoogleTtsApi(Map<String, Object> requestData) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestData, headers);

        RestTemplate restTemplate = new RestTemplate();
        String url = "https://texttospeech.googleapis.com/v1/text:synthesize?key=" + googleTtsApiKey;

        return restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
    }

    private Map<String, Object> processApiResponse(ResponseEntity<Map<String, Object>> response) {

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new SimulationException(ErrorCode.SIM_TTS_API_CALL_FAILED,
                    "Google TTS API 응답 오류: " + response.getStatusCode());
        }

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new SimulationException(ErrorCode.SIM_TTS_API_CALL_FAILED,
                    "Google TTS API 응답 본문이 비어있습니다.");
        }

        String audioContent = (String) responseBody.get("audioContent");
        if (audioContent == null) {
            throw new SimulationException(ErrorCode.SIM_TTS_AUDIO_CONVERSION_FAILED);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("audioContent", audioContent);
        result.put("success", true);
        return result;
    }

}
