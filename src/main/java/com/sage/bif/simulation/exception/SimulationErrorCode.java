package com.sage.bif.simulation.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SimulationErrorCode {
    SIM_NOT_FOUND("시뮬레이션을 찾을 수 없습니다.", "존재하지 않는 simulationId로 시뮬레이션 목록/시작 요청"),
    SESSION_NOT_FOUND("시뮬레이션 세션을 찾을 수 없습니다.", "존재하지 않는 sessionId로 시뮬레이션 진행/결과 요청"),
    SESSION_CREATION_FAILED("시뮬레이션 세션 생성에 실패했습니다.", "이벤트 기반 세션 생성 과정에서 오류 발생"),
    SIMULATION_NOT_COMPLETED("시뮬레이션이 아직 완료되지 않았습니다.", "완료되지 않은 시뮬레이션의 결과를 조회하려고 시도"),
    SIM_INVALID_CHOICE("유효하지 않은 선택입니다.", "현재 시뮬레이션 단계에서 허용되지 않는 선택지를 제출"),
    SIM_INTERNAL_PROCESSING_ERROR("시뮬레이션 처리 중 오류가 발생했습니다.", "시뮬레이션 스크립트 로딩, AI 대화 생성 등 내부 로직 오류");

    private final String message;
    private final String description;
}