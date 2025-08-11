package com.sage.bif.stats.repository;

import com.sage.bif.stats.entity.Stats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StatsRepository extends JpaRepository<Stats, Long> {

    Optional<Stats> findByBifIdAndYearMonth(final Long bifId, final LocalDateTime yearMonth);

    List<Stats> findByBifIdAndYearMonthBetween(final Long bifId, final LocalDateTime startYearMonth, final LocalDateTime endYearMonth);

    @Query("SELECT s FROM Stats s WHERE s.bifId = :bifId ORDER BY s.yearMonth DESC")
    List<Stats> findRecentStatsByBifId(@Param("bifId") final Long bifId, @Param("limit") final int limit);

    @Query("SELECT s FROM Stats s WHERE s.bifId = :bifId ORDER BY s.yearMonth DESC")
    List<Stats> findByBifIdOrderByYearMonthDesc(@Param("bifId") final Long bifId);

    boolean existsByBifIdAndYearMonth(final Long bifId, final LocalDateTime yearMonth);

    void deleteByBifId(final Long bifId);
}
