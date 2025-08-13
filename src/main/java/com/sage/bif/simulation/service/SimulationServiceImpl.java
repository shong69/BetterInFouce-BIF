package com.sage.bif.simulation.service;

import com.sage.bif.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;

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
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationServiceImpl implements SimulationService {

    private final SimulationRepository simulationRepository;
    private final SimulationStepRepository stepRepository;
    private final BifChoiceRepository choiceRepository;
    private final SimulationFeedbackRepository feedbackRepository;
    private final SimulationRecommendationRepository recommendationRepository;
    private final GuardianRepository guardianRepository;
    private final BifRepository bifRepository;

    @Override
    public List<SimulationResponse> getAllSimulations() {
        log.info("시뮬레이션 목록 조회 요청");
        List<Simulation> simulations = simulationRepository.findAll();
        List<SimulationResponse> responses = simulations.stream()
                .map(SimulationResponse::from)
                .toList();
        log.info("시뮬레이션 목록 조회 완료: {}개", responses.size());
        return responses;
    }

    @Override
    public String startSimulation(Long simulationId) {
        log.info("시뮬레이션 시작 요청: simulationId={}", simulationId);

        simulationRepository.findById(simulationId)
                .orElseThrow(() -> new SimulationException(ErrorCode.SIM_NOT_FOUND));

        String sessionId = "session_" + System.currentTimeMillis() + "_" + simulationId + "_1";
        log.info("시뮬레이션 세션 생성: sessionId={}", sessionId);

        return sessionId;
    }

    @Override
    public SimulationChoiceResponse submitChoice(String sessionId, String userChoice) {
        try {
            log.info("=== 서비스 submitChoice 호출 ===");
            log.info("sessionId: {}", sessionId);
            log.info("choice: {}", userChoice);

            Long simulationId = extractSimulationIdFromSessionId(sessionId);
            int currentStep = extractCurrentStepFromSessionId(sessionId);

            log.info("추출된 simulationId: {}", simulationId);
            log.info("추출된 currentStep: {}", currentStep);
        
        SimulationStep currentStepData = stepRepository.findBySimulationIdAndStepOrder(simulationId, currentStep)
                .orElseThrow(() -> new SimulationException(ErrorCode.SIM_NOT_FOUND));
        
        List<BifChoice> choices = choiceRepository.findByStepIdOrderByChoiceId(currentStepData.getStepId());

        log.info("=== 선택지 매칭 디버깅 ===");
        log.info("사용자가 선택한 텍스트: '{}'", userChoice);
        log.info("사용자가 선택한 텍스트 길이: {}", userChoice.length());
        log.info("데이터베이스의 선택지들:");
        choices.forEach(choice -> {
            log.info("  - '{}' (ID: {}, 길이: {})", choice.getChoiceText(), choice.getChoiceId(), choice.getChoiceText().length());
        });

        BifChoice selectedChoice = choices.stream()
                .findFirst()
                .orElseThrow(() -> {
                    log.error("선택지가 없습니다!");
                    return new SimulationException(ErrorCode.SIM_INVALID_CHOICE);
                });

        log.info("선택된 선택지: '{}' (ID: {})", selectedChoice.getChoiceText(), selectedChoice.getChoiceId());

        int currentChoiceScore = selectedChoice.getChoiceScore();
        int totalScore = calculateTotalScore(sessionId, currentChoiceScore);

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
        } catch (Exception e) {
            log.error("서비스에서 예외 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public String getFeedbackText(Long simulationId, int score) {
        log.info("피드백 조회 요청: simulationId={}, score={}", simulationId, score);

        List<SimulationFeedback> allFeedbacks = feedbackRepository.findBySimulationId(simulationId);
        log.info("시뮬레이션 {}의 전체 피드백 개수: {}", simulationId, allFeedbacks.size());

        allFeedbacks.forEach(feedback ->
                log.info("피드백 ID: {}, 점수 범위: {}~{}, 피드백: {}",
                        feedback.getFeedbackId(), feedback.getMinScore(), feedback.getMaxScore(), feedback.getFeedbackText())
        );

        Optional<SimulationFeedback> feedback = feedbackRepository.findBySimulationIdAndScore(simulationId, score);

        if (feedback.isPresent()) {
            log.info("피드백 조회 성공: {}", feedback.get().getFeedbackText());
            return feedback.get().getFeedbackText();
        }

        log.warn("정확한 점수 범위 매칭 실패, 가장 가까운 피드백 찾기 시도");

        Optional<SimulationFeedback> closestFeedback = allFeedbacks.stream()
                .filter(f -> score >= f.getMinScore() && score <= f.getMaxScore())
                .findFirst();

        if (closestFeedback.isPresent()) {
            log.info("가장 가까운 피드백 찾음: {}", closestFeedback.get().getFeedbackText());
            return closestFeedback.get().getFeedbackText();
        }

        log.warn("점수 {}에 맞는 피드백을 찾을 수 없음, 기본 메시지 반환", score);
        return "피드백을 찾을 수 없습니다. 점수: " + score;
    }

    @Override
    public SimulationDetailsResponse getSimulationDetails(Long simulationId) {
        log.info("시뮬레이션 상세 정보 조회 요청: simulationId={}", simulationId);

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

        log.info("시뮬레이션 상세 정보 조회 완료: {}단계", steps.size());

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
        log.info("시뮬레이션 추천 이모티콘 클릭: guardianId={}, bifId={}, simulationId={}", guardianId, bifId, simulationId);

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

            log.info("이모티콘 클릭: recommendationId={}, isActive={}", recommendation.getRecommendationId(), isActive);
        } else {

            SimulationRecommendation newRecommendation = SimulationRecommendation.builder()
                    .guardian(guardian)
                    .bif(bif)
                    .simulation(simulation)
                    .isActive(true)
                    .build();

            recommendationRepository.save(newRecommendation);
            isActive = true;

            log.info("새 추천 생성: recommendationId={}, isActive={}", newRecommendation.getRecommendationId(), isActive);
        }

        List<Long> recommendedSimulationIds = getActiveRecommendationIdsForBif(bifId);

        return SimulationRecommendationResponse.builder()
                .recommendedSimulationIds(recommendedSimulationIds)
                .isActive(isActive)
                .build();
    }

    @Override
    public List<Long> getActiveRecommendationIdsForBif(Long bifId) {
        log.info("BIF 활성 추천 목록 조회: bifId={}", bifId);

        List<SimulationRecommendation> activeRecommendations = recommendationRepository
                .findByBifBifIdAndIsActiveTrue(bifId);

        List<Long> recommendedSimulationIds = activeRecommendations.stream()
                .map(recommendation -> recommendation.getSimulation().getId())
                .toList();

        log.info("활성 추천 개수: {}", recommendedSimulationIds.size());

        return recommendedSimulationIds;
    }

    @Override
    public void recommendSimulation(Long simulationId) {
        log.info("시뮬레이션 추천 기능 호출(구현 예정)");
        throw new UnsupportedOperationException("시뮬레이션 추천 기능은 아직 구현되지 않았습니다.");
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

    private int calculateTotalScore(String sessionId, int currentChoiceScore) {
        // 현재는 단순히 현재 점수만 반환 (클라이언트에서 누적 처리)
        log.info("점수 계산: sessionId={}, currentChoiceScore={}", sessionId, currentChoiceScore);
        return currentChoiceScore;
    }

}
