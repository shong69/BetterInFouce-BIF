package com.sage.bif.simulation.repository;

import com.sage.bif.simulation.entity.SimulationStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SimulationStepRepository extends JpaRepository<SimulationStep, Long> {

    List<SimulationStep> findBySimulationIdOrderByStepOrder(Long simulationId);

    Optional<SimulationStep> findBySimulationIdAndStepOrder(Long simulationId, Integer stepOrder);

}
