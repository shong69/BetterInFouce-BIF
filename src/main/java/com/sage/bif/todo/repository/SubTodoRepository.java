package com.sage.bif.todo.repository;

import com.sage.bif.todo.entity.SubTodo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubTodoRepository extends JpaRepository<SubTodo, Long> {

    @Modifying
    @Query("DELETE FROM SubTodo s WHERE s.todo.bifUser.bifId = :bifId")
    int deleteByTodo_BifUser_BifId(@Param("bifId") Long bifId);

}
