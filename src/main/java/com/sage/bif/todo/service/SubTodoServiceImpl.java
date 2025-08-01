package com.sage.bif.todo.service;

import com.sage.bif.todo.dto.request.SubTodoUpdateRequest;
import com.sage.bif.todo.entity.SubTodo;
import com.sage.bif.todo.repository.SubTodoRepository;
import com.sage.bif.todo.repository.TodoRepository;
import com.sage.bif.todo.exception.SubTodoNotFoundException;
import com.sage.bif.todo.exception.UnauthorizedSubTodoAccessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubTodoServiceImpl implements SubTodoService {

    private final SubTodoRepository subTodoRepository;
    private final TodoRepository todoRepository;

    @Override
    @Transactional
    public void updateSubTodoCompletionStatus(Long bifId, Long subTodoId, boolean isCompleted) {

        SubTodo subTodo = subTodoRepository.findById(subTodoId).orElseThrow(() -> new SubTodoNotFoundException(subTodoId));

        if (!subTodo.getTodoId().getBifId().getId().equals(bifId)) {
            throw new UnauthorizedSubTodoAccessException(bifId, subTodoId);
        }

        subTodo.setIsCompleted(isCompleted);
        if (isCompleted) {
            subTodo.setCompletedAt(LocalDateTime.now());
        } else {
            subTodo.setCompletedAt(null);
        }

    }

    @Override
    @Transactional
    public void updateSubTodo(Long bifId, Long subTodoId, SubTodoUpdateRequest request) {

        SubTodo subTodo = subTodoRepository.findById(subTodoId).orElseThrow(() -> new SubTodoNotFoundException(subTodoId));

        if (!subTodo.getTodoId().getBifId().getId().equals(bifId)) {
            throw new UnauthorizedSubTodoAccessException(bifId, subTodoId);
        }

        subTodo.setTitle(request.getTitle());
        subTodo.setSortOrder(request.getSortOrder());

    }

}
