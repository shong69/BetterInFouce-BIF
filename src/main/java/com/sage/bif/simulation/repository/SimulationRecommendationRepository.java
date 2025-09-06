package com.sage.bif.simulation.repository;

import com.sage.bif.simulation.entity.SimulationRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SimulationRecommendationRepository extends JpaRepository<SimulationRecommendation, Long> {

    Optional<SimulationRecommendation> findByGuardianGuardianIdAndBifBifIdAndSimulationId(
            Long guardianId, Long bifId, Long simulationId);

    @Modifying
    @Query("DELETE FROM SimulationRecommendation sr WHERE sr.bif.bifId = :bifId")
    int deleteByBif_BifId(@Param("bifId") Long bifId);

    @Modifying
    @Query("DELETE FROM SimulationRecommendation sr WHERE sr.guardian IN " +
            "(SELECT g FROM Guardian g WHERE g.socialLogin.socialId = :socialId)")
    int deleteByGuardianSocialId(@Param("socialId") Long socialId);

}
