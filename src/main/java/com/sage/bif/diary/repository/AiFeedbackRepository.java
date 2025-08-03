package com.sage.bif.diary.repository;

import com.sage.bif.diary.entity.AiFeedback;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiFeedbackRepository extends JpaRepository<AiFeedback,Long> {
    Optional<AiFeedback> findByDiaryId(Long diaryId);
}
