package com.sage.bif.todo.repository;

import com.sage.bif.todo.entity.SubTodoCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubTodoCompletionRepository extends JpaRepository<SubTodoCompletion, Long> {

    @Query("SELECT stc FROM SubTodoCompletion stc WHERE stc.subTodo.todo.todoId = :todoId AND stc.completionDate = :completionDate")
    List<SubTodoCompletion> findByTodoIdAndCompletionDate(@Param("todoId") Long todoId, @Param("completionDate") LocalDate completionDate);

    @Modifying
    @Query(value = "INSERT IGNORE INTO sub_todo_completions (sub_todo_id, completion_date, created_at, updated_at) VALUES (:subTodoId, :completionDate, NOW(), NOW())", nativeQuery = true)
    void insertIgnoreCompletion(@Param("subTodoId") Long subTodoId, @Param("completionDate") LocalDate completionDate);

    void deleteBySubTodo_SubTodoIdAndCompletionDate(Long subTodoId, LocalDate completionDate);

    @Modifying
    @Query("DELETE FROM SubTodoCompletion sc WHERE sc.subTodo.todo.bifUser.bifId = :bifId")
    int deleteBySubTodo_Todo_BifUser_BifId(@Param("bifId") Long bifId);

}
