package com.sage.bif.stats.repository;

import com.sage.bif.stats.entity.Stats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface StatsRepository extends JpaRepository<Stats, Long> {

    Optional<Stats> findByBifIdAndYearMonth(final Long bifId, final LocalDateTime yearMonth);

    Optional<Stats> findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(final Long bifId, final LocalDateTime yearMonth);

}
