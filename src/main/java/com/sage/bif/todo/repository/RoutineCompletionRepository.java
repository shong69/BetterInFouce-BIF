package com.sage.bif.todo.repository;

import com.sage.bif.todo.entity.RoutineCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface RoutineCompletionRepository extends JpaRepository<RoutineCompletion, Long> {

    Optional<RoutineCompletion> findByTodo_TodoIdAndCompletionDate(Long todoId, LocalDate completionDate);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM routine_completions WHERE todo_id = ?1 AND completion_date = ?2",
            nativeQuery = true)
    int deleteCompletion(Long todoId, LocalDate completionDate);

    @Modifying
    @Query("DELETE FROM RoutineCompletion r WHERE r.todo.bifUser.bifId = :bifId")
    int deleteByTodo_BifUser_BifId(@Param("bifId") Long bifId);

}
