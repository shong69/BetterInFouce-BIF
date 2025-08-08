package com.sage.bif.simulation.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import com.sage.bif.simulation.entity.Simulation;
import com.sage.bif.simulation.entity.SimulationSession;
import com.sage.bif.simulation.entity.SimulationStep;
import com.sage.bif.simulation.entity.BifChoice;
import com.sage.bif.simulation.entity.SimulationFeedback;
import com.sage.bif.simulation.repository.SimulationRepository;
import com.sage.bif.simulation.repository.SimulationSessionRepository;
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
    private final SimulationSessionRepository sessionRepository;
    private final SimulationStepRepository stepRepository;
    private final BifChoiceRepository choiceRepository;
    private final SimulationFeedbackRepository feedbackRepository;

    
    // 세션 ID 생성은 이벤트 리스너에서 처리
    
    @Override
    public List<SimulationResponse> getAllSimulations() {
        // ===== 메인 비즈니스 로직 (서비스에서 직접 처리) =====
        List<Simulation> simulations = simulationRepository.findAll();
        List<SimulationResponse> responses = simulations.stream()
                .map(SimulationResponse::from)
                .collect(Collectors.toList());
        

        
        return responses;
    }
    
    @Override
    public SimulationSessionResponse startSimulation(Long simulationId) {
        // 시뮬레이션 존재 여부 확인
        Simulation simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new SimulationException(SimulationErrorCode.SIM_NOT_FOUND));
        

        
        // ===== 메인 비즈니스 로직 (서비스에서 직접 처리) =====
        // 세션 ID 생성
        String sessionId = generateUniqueSessionId();
        
        // 세션을 DB에 저장
        SimulationSession session = SimulationSession.builder()
                .sessionId(sessionId)
                .simulationId(simulationId)
                .currentStep(1)
                .totalScore(0)
                .isCompleted(false)
                .build();
        
        sessionRepository.save(session);
        
        // ===== 응답 데이터 조회 (읽기 전용) =====
        // 첫 번째 단계의 시나리오와 선택지 조회
        SimulationStep firstStep = stepRepository.findBySimulationIdAndStepOrder(simulationId, 1)
                .orElseThrow(() -> new SimulationException(SimulationErrorCode.SIM_NOT_FOUND));
        
        // 첫 번째 스텝의 선택지들 조회
        List<BifChoice> firstChoices = choiceRepository.findByStepIdOrderByChoiceId(firstStep.getStepId());
        String[] choiceTexts = firstChoices.stream()
                .map(BifChoice::getChoiceText)
                .toArray(String[]::new);
        

        
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
        // 세션 조회
        SimulationSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new SimulationException(SimulationErrorCode.SESSION_NOT_FOUND));
        
        // 현재 단계의 선택지 조회
        SimulationStep currentStep = stepRepository.findBySimulationIdAndStepOrder(
                session.getSimulationId(), session.getCurrentStep())
                .orElseThrow(() -> new SimulationException(SimulationErrorCode.SIM_NOT_FOUND));
        
        // 선택한 선택지의 점수 계산
        List<BifChoice> choices = choiceRepository.findByStepIdOrderByChoiceId(currentStep.getStepId());
        BifChoice selectedChoice = choices.stream()
                .filter(choice -> choice.getChoiceText().equals(request.getChoice()))
                .findFirst()
                .orElseThrow(() -> new SimulationException(SimulationErrorCode.SIM_INVALID_CHOICE));
        
        // 세션 상태 업데이트
        session.setTotalScore(session.getTotalScore() + selectedChoice.getChoiceScore());
        session.setCurrentStep(session.getCurrentStep() + 1);
        
        // 시뮬레이션 완료 여부 확인
        List<SimulationStep> allSteps = stepRepository.findBySimulationIdOrderByStepOrder(session.getSimulationId());
        boolean isCompleted = session.getCurrentStep() > allSteps.size();
        
        // 시뮬레이션 완료 시 세션 상태 업데이트
        if (isCompleted) {
            session.setIsCompleted(true);
        }
        
        sessionRepository.save(session);
        

        
        // 다음 단계 정보 조회 (완료되지 않은 경우)
        String nextScenario = "";
        String[] nextChoices = new String[0];
        
        if (!isCompleted) {
            SimulationStep nextStepData = stepRepository.findBySimulationIdAndStepOrder(
                    session.getSimulationId(), session.getCurrentStep())
                    .orElseThrow(() -> new SimulationException(SimulationErrorCode.SIM_NOT_FOUND));
            
            List<BifChoice> nextStepChoices = choiceRepository.findByStepIdOrderByChoiceId(nextStepData.getStepId());
            nextScenario = nextStepData.getCharacterLine();
            nextChoices = nextStepChoices.stream()
                    .map(BifChoice::getChoiceText)
                    .toArray(String[]::new);
        }
        
        return SimulationChoiceResponse.builder()
                .sessionId(sessionId)
                .selectedChoice(request.getChoice())
                .feedback(selectedChoice.getFeedbackText() != null ? selectedChoice.getFeedbackText() : "")
                .nextScenario(nextScenario)
                .nextChoices(nextChoices)
                .currentScore(session.getTotalScore())
                .isCompleted(isCompleted)
                .build();
    }
    
    @Override
    public SimulationResultResponse getSimulationResult(String sessionId) {
        // DB에서 세션 조회
        SimulationSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new SimulationException(SimulationErrorCode.SESSION_NOT_FOUND));
        
        if (!session.getIsCompleted()) {
            throw new SimulationException(SimulationErrorCode.SIMULATION_NOT_COMPLETED);
        }
        
        // ===== 실제 계산 로직 =====
        int percentage = calculatePercentage(session.getSimulationId(), session.getTotalScore());
        String feedbackMessage = generateDefaultFeedbackByPercentage(percentage);
        String finalGrade = percentage >= 80 ? "최고!" : percentage >= 60 ? "아주 좋아요!" : "노력해봐요!";
        

        
        return new SimulationResultResponse(
                sessionId, 
                finalGrade, 
                List.of("선택지1", "선택지2", "선택지3"), // 임시 선택지
                session.getTotalScore(), 
                percentage, 
                feedbackMessage
        );
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
    
    // 세션 ID 생성 로직
    
    // ===== 계산 로직 =====
    
    // 퍼센테이지 계산 로직
    private int calculatePercentage(Long simulationId, int totalScore) {
        // 시뮬레이션의 총 점수 계산
        List<SimulationStep> steps = stepRepository.findBySimulationIdOrderByStepOrder(simulationId);
        int maxPossibleScore = 0;
        
        for (SimulationStep step : steps) {
            List<BifChoice> choices = choiceRepository.findByStepIdOrderByChoiceId(step.getStepId());
            int maxStepScore = choices.stream()
                    .mapToInt(BifChoice::getChoiceScore)
                    .max()
                    .orElse(0);
            maxPossibleScore += maxStepScore;
        }
        
        if (maxPossibleScore == 0) return 0;
        return (int) Math.round((double) totalScore / maxPossibleScore * 100);
    }
    
    // 퍼센테이지 기반 피드백 생성 로직
    private String generateDefaultFeedbackByPercentage(int percentage) {
        if (percentage >= 90) {
            return "완벽한 선택이었습니다! 당신의 판단력이 뛰어납니다.";
        } else if (percentage >= 80) {
            return "매우 좋은 선택입니다. 대부분의 상황에서 올바른 판단을 하셨네요.";
        } else if (percentage >= 70) {
            return "좋은 선택입니다. 몇 가지 개선할 점이 있지만 전반적으로 잘하셨습니다.";
        } else if (percentage >= 60) {
            return "보통 수준입니다. 더 나은 결과를 위해 노력해보세요.";
        } else if (percentage >= 50) {
            return "개선이 필요합니다. 상황을 더 신중하게 판단해보세요.";
        } else {
            return "많이 부족합니다. 기본적인 상황 판단 능력을 기르는 것이 좋겠습니다.";
        }
    }
    
    // 추천 기능 구현
    @Override
    public void recommendSimulation(Long simulationId) {
        // 추천 로직 구현
    }

    // 고유 세션 ID 생성 메서드
    private String generateUniqueSessionId() {
        return "session_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }
} 