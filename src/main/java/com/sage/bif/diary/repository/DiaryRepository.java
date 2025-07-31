package com.sage.bif.diary.repository;

import com.sage.bif.diary.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {
    
    @Query("SELECT d FROM Diary d WHERE d.user.bifId = :userId AND d.createdAt BETWEEN :startDate AND :endDate AND d.isDeleted = false ORDER BY d.createdAt DESC")
    List<Diary> findByUserIdAndDateBetween(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
} 