package com.sage.bif.simulation.repository;

import com.sage.bif.simulation.entity.SimulationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SimulationFeedbackRepository extends JpaRepository<SimulationFeedback, Long> {
    
    // 시뮬레이션 ID로 모든 피드백 조회
    List<SimulationFeedback> findBySimulationSimulationIdOrderByMinPercentageDesc(Long simulationId);
    
    // 퍼센테이지 범위에 따른 피드백 조회
    @Query("SELECT sf FROM SimulationFeedback sf WHERE sf.simulation.simulationId = :simulationId AND :percentage BETWEEN sf.minPercentage AND sf.maxPercentage")
    Optional<SimulationFeedback> findBySimulationIdAndPercentageRange(@Param("simulationId") Long simulationId, @Param("percentage") Integer percentage);
    
    // 시뮬레이션 ID로 피드백 존재 여부 확인
    boolean existsBySimulationSimulationId(Long simulationId);
}