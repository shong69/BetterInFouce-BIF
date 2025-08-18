package com.sage.bif.simulation.repository;

import com.sage.bif.simulation.entity.Simulation;

import io.lettuce.core.dynamic.annotation.Param;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SimulationRepository extends JpaRepository<Simulation, Long> {

    @Query("SELECT s, CASE WHEN sr.isActive IS NOT NULL THEN sr.isActive ELSE FALSE END " +
           "FROM Simulation s " +
           "LEFT JOIN SimulationRecommendation sr ON sr.simulation.id = s.id " +
           "AND sr.guardian.guardianId = " +
               "COALESCE(:guardianId, " + 
                   "(SELECT g.guardianId FROM Guardian g WHERE g.bif.bifId = :bifId)" + 
               ") " +
           "AND sr.bif.bifId = :bifId")
    List<Object[]> findAllSimulationsWithRecommendationStatus(
            @Param("guardianId") Long guardianId,
            @Param("bifId") Long bifId
    );

}
