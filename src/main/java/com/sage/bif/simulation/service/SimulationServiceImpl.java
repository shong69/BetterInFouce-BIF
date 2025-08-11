package com.sage.bif.simulation.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.sage.bif.simulation.entity.Simulation;
import com.sage.bif.simulation.entity.SimulationStep;
import com.sage.bif.simulation.entity.BifChoice;
import com.sage.bif.simulation.entity.SimulationFeedback;
import com.sage.bif.simulation.repository.SimulationRepository;
import com.sage.bif.simulation.repository.SimulationStepRepository;
import com.sage.bif.simulation.repository.BifChoiceRepository;
import com.sage.bif.simulation.repository.SimulationFeedbackRepository;
import com.sage.bif.simulation.dto.response.SimulationResponse;
import com.sage.bif.simulation.dto.response.SimulationSessionResponse;
import com.sage.bif.simulation.dto.response.SimulationChoiceResponse;
import com.sage.bif.simulation.dto.response.SimulationResultResponse;
import com.sage.bif.simulation.dto.response.SimulationDetailsResponse;
import com.sage.bif.simulation.dto.request.SimulationChoiceRequest;

import com.sage.bif.simulation.exception.SimulationException;
import com.sage.bif.simulation.exception.SimulationErrorCode;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationServiceImpl implements SimulationService {
    
    private final SimulationRepository simulationRepository;
    private final SimulationStepRepository stepRepository;
    private final BifChoiceRepository choiceRepository;
    private final SimulationFeedbackRepository feedbackRepository;

    @Override
    public List<SimulationResponse> getAllSimulations() {
        List<Simulation> simulations = simulationRepository.findAll();
        List<SimulationResponse> responses = simulations.stream()
                .map(SimulationResponse::from)
                .collect(Collectors.toList());
        return responses;
    }
    
    @Override
    public SimulationSessionResponse startSimulation(Long simulationId) {
        Simulation simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new SimulationException(SimulationErrorCode.SIM_NOT_FOUND));
        
        // 첫 번째 단계의 시나리오와 선택지 조회
        SimulationStep firstStep = stepRepository.findBySimulationIdAndStepOrder(simulationId, 1)
                .orElseThrow(() -> new SimulationException(SimulationErrorCode.SIM_NOT_FOUND));
        
        // 첫 번째 스텝의 선택지들 조회
        List<BifChoice> firstChoices = choiceRepository.findByStepIdOrderByChoiceId(firstStep.getStepId());
        String[] choiceTexts = firstChoices.stream()
                .map(BifChoice::getChoiceText)
                .toArray(String[]::new);
        
        // 세션 ID 생성 (형식: session_{timestamp}_{simulationId}_{currentStep})
        String sessionId = "session_" + System.currentTimeMillis() + "_" + simulationId + "_1";
        
        return SimulationSessionResponse.builder()
                .sessionId(sessionId)
                .simulationId(simulationId)
                .simulationTitle(simulation.getTitle())
                .category(simulation.getCategory())
                .currentStep("1")
                .scenario(firstStep.getCharacterLine())
                .choices(choiceTexts)
                .isCompleted(false)
                .build();
    }
    
        @Override
    public SimulationChoiceResponse submitChoice(String sessionId, SimulationChoiceRequest request) {
        try {
            System.out.println("=== 서비스 submitChoice 호출 ===");
            System.out.println("sessionId: " + sessionId);
            System.out.println("choice: " + request.getChoice());
            
            // 프론트에서 전달받은 정보로 처리 (로컬스토리지 기반)
            // sessionId에서 simulationId 추출 (예: "session_123_1" -> 1)
            Long simulationId = extractSimulationIdFromSessionId(sessionId);
            int currentStep = extractCurrentStepFromSessionId(sessionId);
            
            System.out.println("추출된 simulationId: " + simulationId);
            System.out.println("추출된 currentStep: " + currentStep);
        
        // 현재 단계의 선택지 조회
        SimulationStep currentStepData = stepRepository.findBySimulationIdAndStepOrder(simulationId, currentStep)
                .orElseThrow(() -> new SimulationException(SimulationErrorCode.SIM_NOT_FOUND));
        
        // 선택한 선택지의 점수 계산
        List<BifChoice> choices = choiceRepository.findByStepIdOrderByChoiceId(currentStepData.getStepId());
        BifChoice selectedChoice = choices.stream()
                .filter(choice -> choice.getChoiceText().equals(request.getChoice()))
                .findFirst()
                .orElseThrow(() -> new SimulationException(SimulationErrorCode.SIM_INVALID_CHOICE));
        
        // 다음 단계 정보 조회
        int nextStep = currentStep + 1;
        List<SimulationStep> allSteps = stepRepository.findBySimulationIdOrderByStepOrder(simulationId);
        boolean isCompleted = nextStep > allSteps.size();
        
        String nextScenario = "";
        String[] nextChoices = new String[0];
        
        if (!isCompleted) {
            SimulationStep nextStepData = stepRepository.findBySimulationIdAndStepOrder(simulationId, nextStep)
                    .orElseThrow(() -> new SimulationException(SimulationErrorCode.SIM_NOT_FOUND));
            
            List<BifChoice> nextStepChoices = choiceRepository.findByStepIdOrderByChoiceId(nextStepData.getStepId());
            nextScenario = nextStepData.getCharacterLine();
            nextChoices = nextStepChoices.stream()
                    .map(BifChoice::getChoiceText)
                    .toArray(String[]::new);
        }
        
        // 다음 단계의 세션 ID 생성
        String nextSessionId = "session_" + System.currentTimeMillis() + "_" + simulationId + "_" + nextStep;
        
        return SimulationChoiceResponse.builder()
                .sessionId(nextSessionId)
                .selectedChoice(request.getChoice())
                .feedback(selectedChoice.getFeedbackText() != null ? selectedChoice.getFeedbackText() : "")
                .nextScenario(nextScenario)
                .nextChoices(nextChoices)
                .currentScore(selectedChoice.getChoiceScore())
                .isCompleted(isCompleted)
                .build();
        } catch (Exception e) {
            System.err.println("서비스에서 예외 발생: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    @Override
    public String getFeedbackText(Long simulationId, int score) {
        // DB에서 점수 범위에 맞는 피드백을 찾기
        Optional<SimulationFeedback> feedback = feedbackRepository.findBySimulationIdAndScore(simulationId, score);
        
        if (feedback.isPresent()) {
            return feedback.get().getFeedbackText();
        }
        
        // 매칭되는 피드백이 없으면 기본 문구 반환
        return "피드백 에러입니다.";
    }
    
    @Override
    public SimulationDetailsResponse getSimulationDetails(Long simulationId) {
        // 시뮬레이션 존재 여부 확인
        Simulation simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new SimulationException(SimulationErrorCode.SIM_NOT_FOUND));
        
        // 시뮬레이션 스텝 목록 조회
        List<SimulationStep> steps = stepRepository.findBySimulationIdOrderByStepOrder(simulationId);
        
        // 각 단계별 정보 구성
        List<SimulationDetailsResponse.SimulationStepResponse> stepResponses = steps.stream()
                .map(step -> {
                    // 해당 스텝의 선택지들 조회
                    List<BifChoice> choices = choiceRepository.findByStepIdOrderByChoiceId(step.getStepId());
                    
                    List<SimulationDetailsResponse.SimulationChoiceOptionResponse> choiceOptions = choices.stream()
                            .map(choice -> SimulationDetailsResponse.SimulationChoiceOptionResponse.builder()
                                    .choiceText(choice.getChoiceText())
                                    .choiceScore(choice.getChoiceScore())
                                    .feedbackText(choice.getFeedbackText() != null ? choice.getFeedbackText() : "")
                                    .build())
                            .collect(Collectors.toList());
                    
                    return SimulationDetailsResponse.SimulationStepResponse.builder()
                            .stepNumber(step.getStepOrder())
                            .scenarioText(step.getCharacterLine())
                            .choices(choiceOptions)
                            .build();
                })
                .collect(Collectors.toList());
        
        return SimulationDetailsResponse.builder()
                .simulationId(simulationId)
                .simulationTitle(simulation.getTitle())
                .description(simulation.getDescription())
                .category(simulation.getCategory())
                .totalSteps(steps.size())
                .steps(stepResponses)
                .build();
    }
    
    public void saveScore(String sessionId, int score) {
        // 로컬스토리지에서 처리하므로 서버에서는 불필요
        log.info("점수 저장 요청: sessionId={}, score={} (로컬스토리지에서 처리)", sessionId, score);
    }

    // 추천 기능 구현
    @Override
    public void recommendSimulation(Long simulationId) {
        // 추천 로직 구현
    }

    // 세션 ID에서 시뮬레이션 ID 추출
    private Long extractSimulationIdFromSessionId(String sessionId) {
        // 예: "session_1754836913942_7_1" -> 7
        String[] parts = sessionId.split("_");
        return Long.parseLong(parts[parts.length - 2]);
    }
    
    // 세션 ID에서 현재 단계 추출
    private int extractCurrentStepFromSessionId(String sessionId) {
        // 예: "session_1754836913942_7_1" -> 1
        String[] parts = sessionId.split("_");
        return Integer.parseInt(parts[parts.length - 1]);
    }
    

} 