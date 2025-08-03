package com.sage.bif.todo.repository;

import com.sage.bif.todo.entity.RoutineCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface RoutineCompletionRepository extends JpaRepository<RoutineCompletion, Long> {

    Optional<RoutineCompletion> findByTodo_TodoIdAndCompletionDate(Long todoId, LocalDate completionDate);

    int deleteByTodo_TodoIdAndCompletionDate(Long todoId, LocalDate completionDate);

}
