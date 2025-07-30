package com.sage.bif.stats.repository;

import com.sage.bif.stats.entity.EmotionAnalysisTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmotionAnalysisTemplateRepository extends JpaRepository<EmotionAnalysisTemplate, Long> {

    @Query("SELECT t FROM EmotionAnalysisTemplate t WHERE t.okayRange = :okayRange AND t.goodRange = :goodRange AND t.angryRange = :angryRange AND t.downRange = :downRange AND t.greatRange = :greatRange")
    Optional<EmotionAnalysisTemplate> findByEmotionRanges(@Param("okayRange") final String okayRange,
                                                          @Param("goodRange") final String goodRange,
                                                          @Param("angryRange") final String angryRange,
                                                          @Param("downRange") final String downRange,
                                                          @Param("greatRange") final String greatRange);

    @Query("SELECT t FROM EmotionAnalysisTemplate t ORDER BY RAND() LIMIT 1")
    Optional<EmotionAnalysisTemplate> findRandomTemplate();

    List<EmotionAnalysisTemplate> findAll();
} 