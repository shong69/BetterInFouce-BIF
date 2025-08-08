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
    
    // 시뮬레이션별 피드백 목록 조회
    List<SimulationFeedback> findBySimulationId(Long simulationId);
    
    // 시뮬레이션별 첫 번째 피드백 조회
    Optional<SimulationFeedback> findFirstBySimulationId(Long simulationId);
    
    // 점수 범위에 해당하는 피드백 조회
    @Query("SELECT sf FROM SimulationFeedback sf WHERE sf.simulationId = :simulationId AND :score BETWEEN sf.minScore AND sf.maxScore")
    Optional<SimulationFeedback> findBySimulationIdAndScore(@Param("simulationId") Long simulationId, @Param("score") Integer score);
} 