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
import com.sage.bif.simulation.dto.response.SimulationRecommendationResponse;
import com.sage.bif.simulation.service.SimulationService;
import com.sage.bif.user.entity.Guardian;
import com.sage.bif.user.repository.GuardianRepository;
import com.sage.bif.common.dto.CustomUserDetails;
import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
import com.sage.bif.common.jwt.JwtTokenProvider;
import org.springframework.security.core.Authentication;
import com.sage.bif.user.repository.GuardianRepository;

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
    private final GuardianRepository guardianRepository;

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

            log.info("추출된 simulationId: {}", simulationId);
            log.info("최종 총점: {}", totalScore);

            String feedbackText = simulationService.getFeedbackText(simulationId, totalScore);
            
            log.info("반환할 피드백: {}", feedbackText);
            
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
    
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> completeSimulation(
            @RequestBody Map<String, Object> request) {
        try {
            log.info("=== 시뮬레이션 완료 요청 ===");
            log.info("요청 데이터: {}", request);
            
            String sessionId = (String) request.get("sessionId");
            Object totalScoreObj = request.get("totalScore");

            log.info("sessionId: {}", sessionId);
            log.info("totalScoreObj: {} (타입: {})", totalScoreObj, totalScoreObj != null ? totalScoreObj.getClass().getSimpleName() : "null");

            Long simulationId = extractSimulationIdFromSessionId(sessionId);
            int totalScore;
            
            if (totalScoreObj instanceof Integer integer) {
                totalScore = integer;
            } else if (totalScoreObj instanceof Number number) {
                totalScore = number.intValue();
            } else if (totalScoreObj instanceof String string) {
                try {
                    totalScore = Integer.parseInt(string);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("totalScore가 숫자 형식이 아닙니다: " + string);
                }
            } else if (totalScoreObj == null) {
                totalScore = 0;
            } else {
                throw new IllegalArgumentException("totalScore가 올바른 형식이 아닙니다: " + totalScoreObj);
            }

            log.info("추출된 simulationId: {}", simulationId);
            log.info("최종 총점: {}", totalScore);

            String feedbackText = simulationService.getFeedbackText(simulationId, totalScore);
            
            log.info("반환할 피드백: {}", feedbackText);
            
            Map<String, Object> result = Map.of(
                    "simulationId", simulationId,
                    "totalScore", totalScore,
                    "feedbackText", feedbackText,
                    "success", true
            );
            
            return ResponseEntity.ok(ApiResponse.success(result, "시뮬레이션 완료 성공"));
        } catch (Exception e) {
            log.error("시뮬레이션 완료 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @PostMapping("/recommendations")
    public ResponseEntity<ApiResponse<?>> handleRecommendations(
            @RequestBody(required = false) Map<String, Object> request,
            Authentication authentication) {
        
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("인증되지 않은 사용자입니다."));
            }
            
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getRole() == JwtTokenProvider.UserRole.BIF) {
                if (request == null || request.isEmpty()) {
                    log.info("BIF 추천 목록 조회 요청");
                    List<Long> recommendedSimulationIds = simulationService.getActiveRecommendationIdsForBif(userDetails.getBifId());
                    log.info("BIF 활성 추천 목록 조회 완료: {}개", recommendedSimulationIds.size());
                    return ResponseEntity.ok(ApiResponse.success(recommendedSimulationIds, "추천 목록 조회 완료"));
                } else {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("BIF는 추천 목록만 조회할 수 있습니다."));
                }
            }

            if (userDetails.getRole() == JwtTokenProvider.UserRole.GUARDIAN) {
                if (request == null || request.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("추천할 시뮬레이션 정보가 필요합니다."));
                }
                
                log.info("Guardian 시뮬레이션 추천 요청: {}", request);
                
                Guardian guardian = guardianRepository.findBySocialLogin_SocialId(userDetails.getSocialId())
                        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
                
                Long bifId = Long.valueOf(request.get("bifId").toString());
                Long simulationId = Long.valueOf(request.get("simulationId").toString());
                
                if (!guardian.getBif().getBifId().equals(bifId)) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("연동된 BIF에게만 추천할 수 있습니다."));
                }
                
                SimulationRecommendationResponse response = simulationService.clickRecommendation(
                        guardian.getGuardianId(),
                        bifId,
                        simulationId
                );
                
                log.info("시뮬레이션 추천 완료: isActive={}", response.getIsActive());
                return ResponseEntity.ok(ApiResponse.success(response, "추천 처리 완료"));
            }
            
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("지원하지 않는 사용자 역할입니다."));
            
        } catch (Exception e) {
            log.error("추천 처리 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("추천 처리 중 오류가 발생했습니다: " + e.getMessage()));
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
