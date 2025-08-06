package com.sage.bif.simulation.repository;

import com.sage.bif.simulation.entity.SimulationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface SimulationSessionRepository extends JpaRepository<SimulationSession, Long> {
    
    Optional<SimulationSession> findBySessionId(String sessionId);
    
    List<SimulationSession> findBySimulationId(Long simulationId);
    
    void deleteBySessionId(String sessionId);
    
    boolean existsBySessionId(String sessionId);
}
