package com.sage.bif.todo.repository;

import com.sage.bif.todo.entity.RoutineCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface RoutineCompletionRepository extends JpaRepository<RoutineCompletion, Long> {

    Optional<RoutineCompletion> findByTodo_TodoIdAndCompletionDate(Long todoId, LocalDate completionDate);

    @Modifying
    @Transactional
    @Query(value = "INSERT IGNORE INTO routine_completions (todo_id, completion_date, created_at, updated_at) " +
            "VALUES (?1, ?2, NOW(), NOW())", nativeQuery = true)
    int insertIgnoreCompletion(Long todoId, LocalDate completionDate);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM routine_completions WHERE todo_id = ?1 AND completion_date = ?2",
            nativeQuery = true)
    int deleteCompletion(Long todoId, LocalDate completionDate);

}
