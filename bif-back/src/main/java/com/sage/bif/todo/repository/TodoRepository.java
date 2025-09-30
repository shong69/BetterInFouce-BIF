package com.sage.bif.todo.repository;

import com.sage.bif.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    @Query("SELECT DISTINCT t FROM Todo t LEFT JOIN FETCH t.subTodos s WHERE t.bifUser.bifId = :bifId AND t.isDeleted = false AND (t.dueDate = :date OR t.dueDate IS NULL OR t.type = 'ROUTINE') ORDER BY t.createdAt DESC")
    List<Todo> findTodoWithSubTodosByBifIdAndDate(@Param("bifId") Long bifId, @Param("date") LocalDate date);

    @Query("SELECT DISTINCT t FROM Todo t WHERE t.todoId = :todoId AND t.bifUser.bifId = :bifId AND t.isDeleted = false")
    Optional<Todo> findTodoDetailsById(@Param("bifId") Long bifId, @Param("todoId") Long todoId);

    @Query("SELECT DISTINCT t FROM Todo t LEFT JOIN FETCH t.repeatDays WHERE ((t.type = 'TASK' AND t.dueDate = :date) OR (t.type = 'ROUTINE')) AND t.dueTime IS NOT NULL AND t.notificationEnabled = true AND t.isCompleted = false AND t.isDeleted = false ORDER BY t.dueTime")
    List<Todo> findTodosForNotification(@Param("date") LocalDate date);

    @Modifying
    @Query("DELETE FROM Todo t WHERE t.bifUser.bifId = :bifId")
    int deleteByBifUser_BifId(@Param("bifId") Long bifId);

    @Modifying
    @Query(value = "DELETE FROM todo_repeat_days WHERE todo_id IN (SELECT todo_id FROM todos WHERE bif_id = :bifId)", nativeQuery = true)
    int deleteRepeatDaysByBifId(@Param("bifId") Long bifId);

}
