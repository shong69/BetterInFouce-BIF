package com.sage.bif.diary.repository;

import com.sage.bif.diary.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {

    @Query("SELECT d FROM Diary d LEFT JOIN FETCH d.aiFeedback WHERE d.user.bifId = :userId AND d.createdAt >= :startDate AND d.createdAt < :endDate AND d.isDeleted = false ORDER BY d.createdAt DESC")
    List<Diary> findByUserIdAndDateBetween(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT d FROM Diary d JOIN FETCH d.user LEFT JOIN FETCH d.aiFeedback WHERE d.uuid = :uuid AND d.isDeleted = false")
    Optional<Diary> findByUuidWithUser(@Param("uuid") UUID uuid);

    @Query(value="SELECT COUNT(*) > 0 FROM emotion_diary d WHERE d.bif_id = :userId AND DATE(d.created_at) = :date", nativeQuery = true)
    Long existsByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT d FROM Diary d WHERE d.user.bifId = :userId AND d.isDeleted = false")
    List<Diary> findByUserId(@Param("userId") Long userId);

    Optional<Diary> findByUuid(UUID uuid);
}
