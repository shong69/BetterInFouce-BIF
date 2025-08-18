package com.sage.bif.diary.repository;

import com.sage.bif.diary.entity.AiFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiFeedbackRepository extends JpaRepository<AiFeedback,Long> {
    @Modifying
    @Query("DELETE FROM AiFeedback af WHERE af.diary.id IN :diaryIds")
    int deleteByDiaryIds(@Param("diaryIds") List<Long> diaryIds);

}
