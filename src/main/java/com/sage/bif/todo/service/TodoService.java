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

    TodoUpdatePageResponse updateTodo(Long bifId, Long todoId, TodoUpdateRequest request);

    boolean deleteTodo(Long bifId, Long todoId);

    TodoListResponse updateTodoCompletion(Long bifId, Long todoId, LocalDate completionDate, boolean isCompleted);

    void updateCurrentStep(Long bifId, Long todoId, int newStep);

}
