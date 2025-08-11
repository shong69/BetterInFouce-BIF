package com.sage.bif.simulation.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.sage.bif.simulation.dto.response.SimulationResponse;
import com.sage.bif.simulation.dto.response.SimulationSessionResponse;
import com.sage.bif.simulation.dto.response.SimulationDetailsResponse;

import com.sage.bif.simulation.dto.response.SimulationChoiceResponse;
import com.sage.bif.simulation.dto.response.SimulationResultResponse;
import com.sage.bif.simulation.dto.request.SimulationChoiceRequest;
import com.sage.bif.simulation.service.SimulationService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import com.sage.bif.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import java.util.List;

@RestController
@RequestMapping("/simulations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SimulationController { 

    private final SimulationService simulationService;

    @GetMapping("")
    public ResponseEntity<ApiResponse<List<SimulationResponse>>> getAllSimulations() {
        List<SimulationResponse> simulations = simulationService.getAllSimulations();
        return ResponseEntity.ok(ApiResponse.success(simulations, "시뮬레이션 목록 조회 성공"));
    }
    
    @PostMapping("/{simulationId}/start")
    public ResponseEntity<ApiResponse<SimulationSessionResponse>> startSimulation(@PathVariable Long simulationId) {
        SimulationSessionResponse session = simulationService.startSimulation(simulationId);
        return ResponseEntity.ok(ApiResponse.success(session, "시뮬레이션 시작 성공"));
    }
    
    @PostMapping("/choice")
    public ResponseEntity<ApiResponse<SimulationChoiceResponse>> submitChoice(
            @RequestBody Map<String, Object> request) {
        try {
            System.out.println("=== 컨트롤러 submitChoice 호출 ===");
            System.out.println("요청 데이터: " + request);
            
            String sessionId = (String) request.get("sessionId");
            SimulationChoiceRequest choiceRequest = new SimulationChoiceRequest();
            choiceRequest.setChoice((String) request.get("choice"));
            
            System.out.println("sessionId: " + sessionId);
            System.out.println("choice: " + choiceRequest.getChoice());
            
            SimulationChoiceResponse response = simulationService.submitChoice(sessionId, choiceRequest);
            return ResponseEntity.ok(ApiResponse.success(response, "선택지 제출 성공"));
        } catch (Exception e) {
            System.err.println("컨트롤러에서 예외 발생: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    


    @GetMapping("/{simulationId}/feedback")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFeedback(
            @PathVariable Long simulationId,
            @RequestParam(name = "score", required = false, defaultValue = "0") int score) {
        String text = simulationService.getFeedbackText(simulationId, score);
        Map<String, Object> body = Map.of(
                "simulationId", simulationId,
                "score", score,
                "feedbackText", text
        );
        return ResponseEntity.ok(ApiResponse.success(body, "피드백 조회 성공"));
    }
    @GetMapping("/{simulationId}/details")
    public ResponseEntity<ApiResponse<SimulationDetailsResponse>> getSimulationDetails(@PathVariable Long simulationId) {
        SimulationDetailsResponse details = simulationService.getSimulationDetails(simulationId);
        return ResponseEntity.ok(ApiResponse.success(details, "시뮬레이션 정보 조회 성공"));
    }
    
    @PostMapping("/result")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSimulationResult(
            @RequestBody Map<String, Object> request) {
        try {
            System.out.println("=== /result 엔드포인트 호출 ===");
            System.out.println("요청 데이터: " + request);
            
            String sessionId = (String) request.get("sessionId");
            Object totalScoreObj = request.get("totalScore");
            
            System.out.println("sessionId: " + sessionId);
            System.out.println("totalScoreObj: " + totalScoreObj + " (타입: " + (totalScoreObj != null ? totalScoreObj.getClass().getSimpleName() : "null") + ")");
            
            // sessionId에서 simulationId 추출 (예: "session_1754840583885_6_4" -> 6)
            Long simulationId = extractSimulationIdFromSessionId(sessionId);
            int totalScore;
            
            if (totalScoreObj instanceof Integer) {
                totalScore = (Integer) totalScoreObj;
            } else if (totalScoreObj instanceof Number) {
                totalScore = ((Number) totalScoreObj).intValue();
            } else if (totalScoreObj == null) {
                // totalScore가 null이면 기본값 0 사용
                totalScore = 0;
                System.out.println("totalScore가 null이므로 기본값 0을 사용합니다.");
            } else {
                throw new IllegalArgumentException("totalScore가 올바른 형식이 아닙니다: " + totalScoreObj);
            }
            
            System.out.println("변환된 값 - simulationId: " + simulationId + ", totalScore: " + totalScore);
            
            String feedbackText = simulationService.getFeedbackText(simulationId, totalScore);
            
            Map<String, Object> result = Map.of(
                    "simulationId", simulationId,
                    "totalScore", totalScore,
                    "feedbackText", feedbackText
            );
            
            return ResponseEntity.ok(ApiResponse.success(result, "시뮬레이션 결과 조회 성공"));
        } catch (Exception e) {
            System.err.println("/result 엔드포인트에서 예외 발생: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    private Long extractSimulationIdFromSessionId(String sessionId) {
        // session_{timestamp}_{simulationId}_{currentStep} 형식에서 simulationId 추출
        String[] parts = sessionId.split("_");
        if (parts.length >= 3) {
            return Long.valueOf(parts[2]);
        }
        throw new IllegalArgumentException("잘못된 sessionId 형식: " + sessionId);
    }
    

} 