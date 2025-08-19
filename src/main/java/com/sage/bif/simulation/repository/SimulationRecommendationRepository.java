package com.sage.bif.simulation.repository;

import com.sage.bif.simulation.entity.SimulationRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SimulationRecommendationRepository extends JpaRepository<SimulationRecommendation, Long> {

    Optional<SimulationRecommendation> findByGuardianGuardianIdAndBifBifIdAndSimulationId(
            Long guardianId, Long bifId, Long simulationId
    );

}
