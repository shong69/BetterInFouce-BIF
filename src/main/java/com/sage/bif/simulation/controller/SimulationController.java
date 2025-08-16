package com.sage.bif.simulation.controller;

import com.sage.bif.simulation.exception.SimulationException;
import org.springframework.http.*;
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
import com.sage.bif.user.entity.Bif;
import com.sage.bif.user.entity.Guardian;
import com.sage.bif.user.repository.BifRepository;
import com.sage.bif.user.repository.GuardianRepository;
import com.sage.bif.common.dto.CustomUserDetails;
import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
import com.sage.bif.common.jwt.JwtTokenProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.Map;

import com.sage.bif.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/simulations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SimulationController {

    private static final String TOTAL_SCORE = "totalScore";
    private static final String SIMULATION_ID = "simulationId";
    private static final String FEEDBACK_TEXT = "feedbackText";
    private static final String SESSION_ID = "sessionId";

    private final SimulationService simulationService;
    private final GuardianRepository guardianRepository;
    private final BifRepository bifRepository;

    @GetMapping("")
    public ResponseEntity<ApiResponse<List<SimulationResponse>>> getAllSimulations(
            Authentication authentication,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("인증되지 않은 사용자입니다."));
        }

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("사용자 정보를 가져올 수 없습니다."));
        }

        Long socialId = userDetails.getSocialId();
        String userRole = userDetails.getRole().name();

        if (socialId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("유효하지 않은 사용자 식별자입니다."));
        }

        Long guardianId = null;
        Long bifId = null;
        List<SimulationResponse> simulations;

        if ("GUARDIAN".equals(userRole)) {
            Guardian guardian = guardianRepository.findBySocialLogin_SocialId(socialId)
                    .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND, "가디언 사용자 정보를 찾을 수 없습니다."));
            guardianId = guardian.getGuardianId();

            if (guardian.getBif() != null) {
                bifId = guardian.getBif().getBifId();
            }

            simulations = simulationService.getAllSimulations(guardianId, bifId);

        } else if ("BIF".equals(userRole)) {
            Bif bif = bifRepository.findBySocialLogin_SocialId(socialId)
                    .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND, "BIF 사용자 정보를 찾을 수 없습니다."));
            bifId = bif.getBifId();

            simulations = simulationService.getAllSimulations(null, bifId);

        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("시뮬레이션 접근 권한이 없습니다."));
        }

        return ResponseEntity.ok(ApiResponse.success(simulations, "시뮬레이션 목록 조회 성공"));
    }

    @PostMapping("/{simulationId}/start")
    public ResponseEntity<ApiResponse<String>> startSimulation(@PathVariable Long simulationId) {
        try {
            String sessionId = simulationService.startSimulation(simulationId);
            return ResponseEntity.ok(ApiResponse.success(sessionId, "시뮬레이션 시작 성공"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("시뮬레이션 시작 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/choice")
    public ResponseEntity<ApiResponse<SimulationChoiceResponse>> submitChoice(
            @RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get(SESSION_ID);
            String choice = (String) request.get("choice");

            SimulationChoiceResponse response = simulationService.submitChoice(sessionId, choice);
            return ResponseEntity.ok(ApiResponse.success(response, "선택지 제출 성공"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("선택지 제출 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/{simulationId}/feedback")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFeedback(
            @PathVariable Long simulationId,
            @RequestParam(name = "score", required = false, defaultValue = "0") int score) {
        String text = simulationService.getFeedbackText(simulationId, score);
        Map<String, Object> body = Map.of(
                SIMULATION_ID, simulationId,
                "score", score,
                FEEDBACK_TEXT, text
        );
        return ResponseEntity.ok(ApiResponse.success(body, "피드백 조회 성공"));
    }
    @GetMapping("/{simulationId}/details")
    public ResponseEntity<ApiResponse<SimulationDetailsResponse>> getSimulationDetails(@PathVariable Long simulationId) {
        try {
            SimulationDetailsResponse details = simulationService.getSimulationDetails(simulationId);
            return ResponseEntity.ok(ApiResponse.success(details, "시뮬레이션 정보 조회 성공"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("시뮬레이션 정보 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/result")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSimulationResult(
            @RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get(SESSION_ID);
            Object totalScoreObj = request.get(TOTAL_SCORE);

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
                    SIMULATION_ID, simulationId,
                    TOTAL_SCORE, totalScore,
                    FEEDBACK_TEXT, feedbackText
            );

            return ResponseEntity.ok(ApiResponse.success(result, "시뮬레이션 결과 조회 성공"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("시뮬레이션 결과 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }


    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> completeSimulation(
            @RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get(SESSION_ID);
            Object totalScoreObj = request.get(TOTAL_SCORE);

            Long simulationId = extractSimulationIdFromSessionId(sessionId);
            int totalScore;

            if (totalScoreObj instanceof Integer integer) {
                totalScore = integer;
            } else if (totalScoreObj instanceof Number number) {
                totalScore = number.intValue();
            } else if (totalScoreObj instanceof String string) {
                totalScore = parseTotalScoreFromString(string);
            } else if (totalScoreObj == null) {
                totalScore = 0;
            } else {
                throw new IllegalArgumentException("totalScore가 올바른 형식이 아닙니다: " + totalScoreObj);
            }

            String feedbackText = simulationService.getFeedbackText(simulationId, totalScore);

            Map<String, Object> result = Map.of(
                    SIMULATION_ID, simulationId,
                    TOTAL_SCORE, totalScore,
                    FEEDBACK_TEXT, feedbackText,
                    "success", true
            );

            return ResponseEntity.ok(ApiResponse.success(result, "시뮬레이션 완료 성공"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("시뮬레이션 완료 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/recommendations")
    public ResponseEntity<ApiResponse<Object>> handleRecommendations(
            @RequestBody(required = false) Map<String, Object> request,
            Authentication authentication) {

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("인증되지 않은 사용자입니다."));
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (userDetails.getRole() == JwtTokenProvider.UserRole.GUARDIAN) {
                if (request == null || request.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error("추천할 시뮬레이션 정보가 필요합니다."));
                }

                Guardian guardian = guardianRepository.findBySocialLogin_SocialId(userDetails.getSocialId())
                        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

                Long bifId = guardian.getBif().getBifId();
                Long simulationId = Long.valueOf(request.get(SIMULATION_ID).toString());

                SimulationRecommendationResponse response = simulationService.clickRecommendation(
                        guardian.getGuardianId(),
                        bifId,
                        simulationId
                );

                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "추천 처리 완료"));
            }

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("지원하지 않는 사용자 역할입니다."));

        } catch (BaseException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
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

    private int parseTotalScoreFromString(String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("totalScore가 숫자 형식이 아닙니다: " + string);
        }
    }

    @PostMapping("/tts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> textToSpeech(
            @RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            String voiceName = request.get("voiceName");

            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("텍스트가 필요합니다."));
            }

            Map<String, Object> result = simulationService.convertTextToSpeech(text, voiceName);
            return ResponseEntity.ok(ApiResponse.success(result, "TTS 변환 성공"));
        } catch (SimulationException e) {
            return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("TTS 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

}
