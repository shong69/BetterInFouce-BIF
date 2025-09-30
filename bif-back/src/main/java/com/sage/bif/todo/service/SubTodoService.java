package com.sage.bif.todo.service;

import com.sage.bif.todo.dto.request.SubTodoUpdateRequest;

import java.time.LocalDate;

public interface SubTodoService {

    void updateSubTodoCompletionStatus(Long bifId, Long todoId, Long subTodoId, boolean isCompleted);

    void updateSubTodoCompletionStatus(Long bifId, Long todoId, Long subTodoId, boolean isCompleted, LocalDate completionDate);

    void updateSubTodo(Long bifId, Long todoId, Long subTodoId, SubTodoUpdateRequest request);

}
