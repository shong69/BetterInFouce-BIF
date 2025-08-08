package com.sage.bif.simulation.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.sage.bif.simulation.dto.response.SimulationResponse;
import com.sage.bif.simulation.dto.response.SimulationSessionResponse;
import com.sage.bif.simulation.dto.response.SimulationDetailsResponse;

import com.sage.bif.simulation.dto.response.SimulationChoiceResponse;
import com.sage.bif.simulation.dto.response.SimulationResultResponse;
import com.sage.bif.simulation.dto.request.SimulationChoiceRequest;
import com.sage.bif.simulation.service.SimulationService;
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
    
    @PostMapping("/{sessionId}/choice")
    public ResponseEntity<ApiResponse<SimulationChoiceResponse>> submitChoice(
            @PathVariable String sessionId,
            @RequestBody SimulationChoiceRequest request) {
        SimulationChoiceResponse response = simulationService.submitChoice(sessionId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "선택지 제출 성공"));
    }
    
    @GetMapping("/{sessionId}/result")
    public ResponseEntity<ApiResponse<SimulationResultResponse>> getSimulationResult(@PathVariable String sessionId) {
        SimulationResultResponse result = simulationService.getSimulationResult(sessionId);
        return ResponseEntity.ok(ApiResponse.success(result, "시뮬레이션 결과 조회 성공"));
    }
    
    @GetMapping("/{simulationId}/details")
    public ResponseEntity<ApiResponse<SimulationDetailsResponse>> getSimulationDetails(@PathVariable Long simulationId) {
        SimulationDetailsResponse details = simulationService.getSimulationDetails(simulationId);
        return ResponseEntity.ok(ApiResponse.success(details, "시뮬레이션 정보 조회 성공"));
    }
    
    @PostMapping("/{simulationId}/recommend")
    public ResponseEntity<ApiResponse<String>> recommendSimulation(@PathVariable Long simulationId) {
        simulationService.recommendSimulation(simulationId);
        return ResponseEntity.ok(ApiResponse.success("추천되었습니다.", "시뮬레이션 추천 성공"));
    }
} 