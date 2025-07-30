package com.sage.bif.stats.repository;

import com.sage.bif.stats.entity.Stats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatsRepository extends JpaRepository<Stats, Long> {

    Optional<Stats> findByBifIdAndYearAndMonth(final Long bifId, final Integer year, final Integer month);

    List<Stats> findByBifIdAndYearOrderByMonthAsc(final Long bifId, final Integer year);

    @Query("SELECT s FROM Stats s WHERE s.bifId = :bifId AND s.year = :year AND s.month BETWEEN :startMonth AND :endMonth ORDER BY s.month ASC")
    List<Stats> findByBifIdAndYearAndMonthBetween(@Param("bifId") final Long bifId,
                                                  @Param("year") final Integer year,
                                                  @Param("startMonth") final Integer startMonth,
                                                  @Param("endMonth") final Integer endMonth);

    @Query("SELECT s FROM Stats s WHERE s.bifId = :bifId ORDER BY s.year DESC, s.month DESC LIMIT :limit")
    List<Stats> findRecentStatsByBifId(@Param("bifId") final Long bifId, @Param("limit") final int limit);

    @Query("SELECT s FROM Stats s WHERE s.bifId = :bifId AND s.isCurrentMonth = true")
    Optional<Stats> findCurrentMonthStats(@Param("bifId") final Long bifId);

    @Query("SELECT s FROM Stats s WHERE s.bifId = :bifId AND s.isCurrentMonth = false ORDER BY s.year DESC, s.month DESC")
    List<Stats> findPastMonthStats(@Param("bifId") final Long bifId);

    @Query("SELECT s FROM Stats s WHERE s.bifId = :bifId ORDER BY s.year DESC, s.month DESC")
    List<Stats> findByBifIdOrderByYearMonthDesc(@Param("bifId") final Long bifId);

    boolean existsByBifIdAndYearAndMonth(final Long bifId, final Integer year, final Integer month);

    void deleteByBifId(final Long bifId);
}