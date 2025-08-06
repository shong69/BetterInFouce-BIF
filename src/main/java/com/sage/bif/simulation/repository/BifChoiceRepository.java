package com.sage.bif.simulation.repository;

import com.sage.bif.simulation.entity.BifChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BifChoiceRepository extends JpaRepository<BifChoice, Long> {
    
    List<BifChoice> findByStepStepIdOrderByChoiceId(Long stepId);
    
    List<BifChoice> findBySimulationIdAndStepStepId(Long simulationId, Long stepId);
    
    Optional<BifChoice> findByStepStepIdAndChoiceText(Long stepId, String choiceText);
    
    List<BifChoice> findBySimulationId(Long simulationId);
} 