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
import com.sage.bif.simulation.dto.response.SimulationDetailsResponse;

import com.sage.bif.simulation.dto.response.SimulationChoiceResponse;

import com.sage.bif.simulation.service.SimulationService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import com.sage.bif.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@RestController
@RequestMapping("/simulations")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SimulationController { 

    private final SimulationService simulationService;

    @GetMapping("")
    public ResponseEntity<ApiResponse<List<SimulationResponse>>> getAllSimulations() {
        List<SimulationResponse> simulations = simulationService.getAllSimulations();
        return ResponseEntity.ok(ApiResponse.success(simulations, "시뮬레이션 목록 조회 성공"));
    }
    
    @PostMapping("/{simulationId}/start")
    public ResponseEntity<ApiResponse<String>> startSimulation(@PathVariable Long simulationId) {

        String sessionId = simulationService.startSimulation(simulationId);
        return ResponseEntity.ok(ApiResponse.success(sessionId, "시뮬레이션 시작 성공"));
        
    }
    
    @PostMapping("/choice")
    public ResponseEntity<ApiResponse<SimulationChoiceResponse>> submitChoice(
            @RequestBody Map<String, Object> request) {
        try {
            log.info("=== 컨트롤러 submitChoice 호출 ===");
            log.info("요청 데이터: {}", request);
            
            String sessionId = (String) request.get("sessionId");
            String choice = (String) request.get("choice");

            SimulationChoiceResponse response = simulationService.submitChoice(sessionId, choice);
            return ResponseEntity.ok(ApiResponse.success(response, "선택지 제출 성공"));
        } catch (Exception e) {
            log.error("선택지 제출 중 오류 발생: {}", e.getMessage(), e);
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
            
            String sessionId = (String) request.get("sessionId");
            Object totalScoreObj = request.get("totalScore");

            Long simulationId = extractSimulationIdFromSessionId(sessionId);
            int totalScore;
            
            if (totalScoreObj instanceof Integer integer) {
                totalScore = integer;
            } else if (totalScoreObj instanceof Number number) {
                totalScore = number.intValue();
            } else if (totalScoreObj == null) {
                totalScore = 0;
            } else {
                throw new IllegalArgumentException("totalScore가 올바른 형식이 아닙니다: " + totalScoreObj);
            }

            String feedbackText = simulationService.getFeedbackText(simulationId, totalScore);
            
            Map<String, Object> result = Map.of(
                    "simulationId", simulationId,
                    "totalScore", totalScore,
                    "feedbackText", feedbackText
            );
            
            return ResponseEntity.ok(ApiResponse.success(result, "시뮬레이션 결과 조회 성공"));
        } catch (Exception e) {
            log.error("시뮬레이션 결과 조회 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private Long extractSimulationIdFromSessionId(String sessionId) {
        String[] parts = sessionId.split("_");
        if (parts.length >= 3) {
            return Long.valueOf(parts[2]);
        }
        throw new IllegalArgumentException("잘못된 sessionId 형식: " + sessionId);

    }

}
