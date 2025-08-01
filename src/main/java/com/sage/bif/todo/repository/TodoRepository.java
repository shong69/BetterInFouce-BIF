package com.sage.bif.todo.repository;

import com.sage.bif.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    @Query("SELECT DISTINCT t FROM Todo t LEFT JOIN FETCH t.subTodos s WHERE t.bifId.bifId = :bifId AND t.isDeleted = false AND (t.dueDate = :date OR t.dueDate IS NULL OR t.type = 'ROUTINE') ORDER BY t.createdAt DESC, s.sortOrder ASC")
    List<Todo> findTodoWithSubTodosByBifIdAndDate(@Param("bifId") Long bifId, @Param("date")LocalDate date);

    @Query("SELECT DISTINCT t FROM Todo t LEFT JOIN FETCH t.repeatDays LEFT JOIN FETCH t.subTodos s WHERE t.todoId = :todoId AND t.bifId.bifId = :bifId AND t.isDeleted = false")
    Optional<Todo> findTodoDetailsById(@Param("bifId") Long bifId, @Param("todoId") Long todoId);
}
