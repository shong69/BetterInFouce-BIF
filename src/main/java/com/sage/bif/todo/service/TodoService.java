package com.sage.bif.todo.service;

import com.sage.bif.todo.dto.request.AiTodoCreateRequest;
import com.sage.bif.todo.dto.request.TodoUpdateRequest;
import com.sage.bif.todo.dto.response.TodoListResponse;
import com.sage.bif.todo.dto.response.TodoUpdatePageResponse;

import java.time.LocalDate;
import java.util.List;

public interface TodoService {

    TodoListResponse createTodoByAi(Long bifId, AiTodoCreateRequest request);

    List<TodoListResponse> getTodoList(Long bifId, LocalDate date);

    TodoUpdatePageResponse getTodoDetail(Long bifId, Long todoId);
    
    TodoUpdatePageResponse getTodoDetail(Long bifId, Long todoId, LocalDate viewDate);

    TodoListResponse updateTodo(Long bifId, Long todoId, TodoUpdateRequest request);

    boolean deleteTodo(Long bifId, Long todoId);

    TodoListResponse completeTodo(Long bifId, Long todoId);
    
    TodoListResponse completeTodo(Long bifId, Long todoId, LocalDate completionDate);

    TodoListResponse uncompleteTodo(Long bifId, Long todoId);
    
    TodoListResponse uncompleteTodo(Long bifId, Long todoId, LocalDate targetDate);

    void updateCurrentStep(Long bifId, Long todoId, int newStep);

}
