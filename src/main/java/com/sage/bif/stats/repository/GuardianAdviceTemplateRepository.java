package com.sage.bif.stats.repository;

import com.sage.bif.stats.entity.GuardianAdviceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GuardianAdviceTemplateRepository extends JpaRepository<GuardianAdviceTemplate, Long> {

    @Query("SELECT t FROM GuardianAdviceTemplate t WHERE t.okayRange = :okayRange AND t.goodRange = :goodRange AND t.angryRange = :angryRange AND t.downRange = :downRange AND t.greatRange = :greatRange")
    Optional<GuardianAdviceTemplate> findByEmotionRanges(@Param("okayRange") final String okayRange,
                                                          @Param("goodRange") final String goodRange,
                                                          @Param("angryRange") final String angryRange,
                                                          @Param("downRange") final String downRange,
                                                          @Param("greatRange") final String greatRange);

} 
