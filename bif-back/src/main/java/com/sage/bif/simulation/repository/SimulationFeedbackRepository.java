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

    List<SimulationFeedback> findBySimulationId(Long simulationId);

    @Query("SELECT sf FROM SimulationFeedback sf WHERE sf.simulationId = :simulationId AND :score BETWEEN sf.minScore AND sf.maxScore")
    Optional<SimulationFeedback> findBySimulationIdAndScore(@Param("simulationId") Long simulationId, @Param("score") Integer score);

}
