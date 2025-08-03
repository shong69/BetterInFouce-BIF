package com.sage.bif.todo.repository;

import com.sage.bif.todo.entity.SubTodo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubTodoRepository extends JpaRepository<SubTodo, Long> {

}
