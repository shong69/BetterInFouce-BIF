package com.sage.bif.simulation.service;

import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
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
import com.sage.bif.simulation.event.model.SimulationStartedEvent;
import com.sage.bif.simulation.event.model.SimulationChoiceSubmittedEvent;
import com.sage.bif.simulation.event.model.SimulationCompletedEvent;
import com.sage.bif.simulation.exception.SimulationException;
import com.sage.bif.simulation.exception.SimulationErrorCode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SimulationServiceImpl implements SimulationService {
    
    private final SimulationRepository simulationRepository;
    private final SimulationSessionRepository sessionRepository;
    private final SimulationStepRepository stepRepository;
    private final BifChoiceRepository choiceRepository;
    private final SimulationFeedbackRepository feedbackRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    // 세션 ID autoincrement를 위한 AtomicLong
    private final AtomicLong sessionIdCounter = new AtomicLong(1);
    
    @Override
    public List<SimulationResponse> getAllSimulations() {
        List<Simulation> simulations = simulationRepository.findAll();
        if (simulations.isEmpty()) {
            throw new SimulationException(SimulationErrorCode.SIM_NOT_FOUND);
        }        
        return simulations.stream()
                .map(SimulationResponse::from)
                .collect(Collectors.toList());
    }
    
    @Override
    public SimulationSessionResponse startSimulation(Long simulationId) {
        // 시뮬레이션 존재 여부 확인
        Simulation simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new SimulationException(SimulationErrorCode.SIM_NOT_FOUND));
        
        // 랜덤 세션 ID 생성
        String sessionId = generateUniqueSessionId();
        
        // DB에 세션 저장
        SimulationSession session = SimulationSession.builder()
                .sessionId(sessionId)
                .simulationId(simulationId)
                .currentStep(1)
                .totalScore(0)
                .isCompleted(false)
                .build();
        
        sessionRepository.save(session);
        
        // 첫 번째 단계의 시나리오와 선택지 조회
        SimulationStep firstStep = stepRepository.findBySimulationIdAndStepOrder(simulationId, 1)
                .orElseThrow(() -> new SimulationException(SimulationErrorCode.SIM_NOT_FOUND));
        
        // 첫 번째 스텝의 선택지들 조회
        List<BifChoice> firstChoices = choiceRepository.findByStepStepIdOrderByChoiceId(firstStep.getStepId());
        String[] choiceTexts = firstChoices.stream()
                .map(BifChoice::getChoiceText)
                .toArray(String[]::new);
        
        // 시뮬레이션 시작 이벤트 발행
        eventPublisher.publishEvent(new SimulationStartedEvent(
            this, simulationId, sessionId, 1L, simulation.getTitle(), simulation.getCategory()
        ));
        
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
        // DB에서 세션 조회
        SimulationSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new SimulationException(SimulationErrorCode.SESSION_NOT_FOUND));
        
        // 현재 스텝 조회
        SimulationStep currentStep = stepRepository.findBySimulationIdAndStepOrder(session.getSimulationId(), session.getCurrentStep())
                .orElseThrow(() -> new SimulationException(SimulationErrorCode.SIM_NOT_FOUND));
        
        // 선택한 선택지의 정보 조회
        BifChoice selectedChoice = choiceRepository.findByStepStepIdAndChoiceText(currentStep.getStepId(), request.getChoice())
                .orElseThrow(() -> new SimulationException(SimulationErrorCode.SIM_NOT_FOUND));
        
        // 다음 단계의 시나리오와 선택지 조회
        int nextStep = session.getCurrentStep() + 1;
        SimulationStep nextStepEntity = stepRepository.findBySimulationIdAndStepOrder(session.getSimulationId(), nextStep)
                .orElse(null);
        
        List<BifChoice> nextChoices = new ArrayList<>();
        String[] nextChoiceTexts = new String[0];
        if (nextStepEntity != null) {
            nextChoices = choiceRepository.findByStepStepIdOrderByChoiceId(nextStepEntity.getStepId());
            nextChoiceTexts = nextChoices.stream()
                    .map(BifChoice::getChoiceText)
                    .toArray(String[]::new);
        }
        
        // 선택한 선택지의 실제 점수 사용
        int choiceScore = selectedChoice.getChoiceScore();
        
        // 점수 누적 및 단계 진행
        session.setTotalScore(session.getTotalScore() + choiceScore);
        session.setCurrentStep(session.getCurrentStep() + 1);
        
        // 시뮬레이션의 총 단계 수 조회
        List<SimulationStep> allSteps = stepRepository.findBySimulationIdOrderByStepOrder(session.getSimulationId());
        int totalSteps = allSteps.size();
        
        // 완료 여부 확인 (현재 단계가 총 단계 수를 초과하면 완료)
        boolean isCompleted = session.getCurrentStep() > totalSteps;
        session.setIsCompleted(isCompleted);
        
        // DB에 업데이트 저장
        sessionRepository.save(session);
        
        // 선택지 제출 이벤트 발행
        eventPublisher.publishEvent(new SimulationChoiceSubmittedEvent(
            this, sessionId, session.getSimulationId(), request.getChoice(), 
            session.getCurrentStep(), session.getTotalScore(), 1L
        ));
        
        // 시뮬레이션 완료 이벤트 발행 (완료된 경우)
        if (isCompleted) {
            int percentage = calculatePercentage(session.getSimulationId(), session.getTotalScore());
            String finalGrade = calculateFinalResultByPercentage(percentage);
            Simulation simulation = simulationRepository.findById(session.getSimulationId()).orElse(null);
            String simulationTitle = simulation != null ? simulation.getTitle() : "Unknown";
            
            eventPublisher.publishEvent(new SimulationCompletedEvent(
                this, sessionId, session.getSimulationId(), session.getTotalScore(), 
                finalGrade, 1L, simulationTitle
            ));
        }
        
        return SimulationChoiceResponse.builder()
                .sessionId(sessionId)
                .selectedChoice(request.getChoice())
                .feedback(selectedChoice.getFeedbackText() != null ? selectedChoice.getFeedbackText() : "선택이 완료되었습니다.")
                .nextScenario(nextStepEntity != null ? nextStepEntity.getCharacterLine() : "시뮬레이션이 완료되었습니다.")
                .nextChoices(nextChoiceTexts)
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
        
        // 퍼센테이지 계산
        int percentage = calculatePercentage(session.getSimulationId(), session.getTotalScore());
        
        // 최종 결과 계산 - 퍼센테이지 기반 피드백 사용
        String finalResult = calculateFinalResultByPercentage(percentage);
        List<String> choices = List.of("선택지1", "선택지2", "선택지3");
        String feedback = getFeedbackByPercentageRange(session.getSimulationId(), percentage);
        
        return new SimulationResultResponse(sessionId, finalResult, choices, session.getTotalScore(), percentage, feedback);
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
                    List<BifChoice> choices = choiceRepository.findByStepStepIdOrderByChoiceId(step.getStepId());
                    
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
                .totalSteps(stepResponses.size())
                .steps(stepResponses)
                .build();
    }
    
    // autoincrement 방식으로 고유 세션 ID 생성 메서드
    private String generateUniqueSessionId() {
        long nextId = sessionIdCounter.getAndIncrement();
        return "session_" + nextId;
    }
    
    // 퍼센테이지 계산 메서드
    private int calculatePercentage(Long simulationId, int totalScore) {
        List<SimulationStep> allSteps = stepRepository.findBySimulationIdOrderByStepOrder(simulationId);
        
        // 각 단계의 최대 점수 계산 (각 단계에서 최고 점수 선택지의 점수)
        int maxPossibleScore = 0;
        for (SimulationStep step : allSteps) {
            List<BifChoice> stepChoices = choiceRepository.findByStepStepIdOrderByChoiceId(step.getStepId());
            int stepMaxScore = stepChoices.stream()
                    .mapToInt(BifChoice::getChoiceScore)
                    .max()
                    .orElse(0);
            maxPossibleScore += stepMaxScore;
        }
        
        // 퍼센테이지 계산 (실제 획득 점수 / 최대 가능 점수 * 100)
        return maxPossibleScore > 0 ? (totalScore * 100) / maxPossibleScore : 0;
    }
    
    // 퍼센테이지 기반 최종 결과 계산 (모든 단계를 맞췄을 때만 최고 등급)
    private String calculateFinalResultByPercentage(int percentage) {
        if (percentage >= 100) return "완벽한 성과입니다! 모든 단계를 완벽하게 해결하셨네요!";
        else if (percentage >= 90) return "훌륭한 성과입니다! 거의 모든 단계를 잘 해결하셨어요!";
        else if (percentage >= 80) return "매우 좋은 성과입니다! 대부분의 단계를 잘 해결하셨어요!";
        else if (percentage >= 70) return "좋은 성과입니다. 몇몇 단계에서 개선의 여지가 있어요.";
        else if (percentage >= 60) return "양호한 성과입니다. 더 많은 연습이 필요해요.";
        else if (percentage >= 50) return "보통의 성과입니다. 개선의 여지가 많아요.";
        else return "개선이 필요합니다. 꾸준히 연습해보세요!";
    }
    
    // 퍼센테이지 기반 피드백 조회 메서드
    private String getFeedbackByPercentageRange(Long simulationId, int percentage) {
        try {
            // DB에서 퍼센테이지 범위에 맞는 피드백 조회
            Optional<SimulationFeedback> feedback = feedbackRepository.findBySimulationIdAndPercentageRange(simulationId, percentage);
            
            if (feedback.isPresent()) {
                return feedback.get().getFeedbackText();
            } else {
                // 피드백이 없는 경우 기본 피드백 반환
                return generateDefaultFeedbackByPercentage(percentage);
            }
        } catch (Exception e) {
            // 예외 발생 시 기본 피드백 반환
            return generateDefaultFeedbackByPercentage(percentage);
        }
    }
    
    // 퍼센테이지 기반 기본 피드백 생성 (DB에 피드백이 없는 경우)
    private String generateDefaultFeedbackByPercentage(int percentage) {
        if (percentage >= 100) return "완벽한 결과입니다! 모든 단계를 완벽하게 해결하셨네요!";
        else if (percentage >= 90) return "훌륭한 결과입니다! 거의 모든 단계를 잘 해결하셨어요!";
        else if (percentage >= 80) return "매우 좋은 결과입니다! 대부분의 단계를 잘 해결하셨어요!";
        else if (percentage >= 70) return "좋은 결과입니다. 몇몇 단계에서 개선의 여지가 있어요.";
        else if (percentage >= 60) return "양호한 결과입니다. 더 많은 연습이 필요해요.";
        else if (percentage >= 50) return "보통의 결과입니다. 개선의 여지가 많아요.";
        else return "개선이 필요합니다. 꾸준히 연습해보세요!";
    }
} 