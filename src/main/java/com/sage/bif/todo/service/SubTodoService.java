package com.sage.bif.todo.service;

import com.sage.bif.todo.dto.request.SubTodoUpdateRequest;

public interface SubTodoService {

    void updateSubTodoCompletionStatus(Long bifId, Long subTodoId, boolean isCompleted);

    void updateSubTodo(Long bifId, Long subTodoId, SubTodoUpdateRequest request);

}
