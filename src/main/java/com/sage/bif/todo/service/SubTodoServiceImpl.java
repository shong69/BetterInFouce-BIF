package com.sage.bif.todo.service;

import com.sage.bif.todo.dto.request.SubTodoUpdateRequest;
import com.sage.bif.todo.entity.SubTodo;
import com.sage.bif.todo.exception.InvalidSubTodoRelationException;
import com.sage.bif.todo.exception.SubTodoNotFoundException;
import com.sage.bif.todo.exception.UnauthorizedSubTodoAccessException;
import com.sage.bif.todo.repository.SubTodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class SubTodoServiceImpl implements SubTodoService {

    private final SubTodoRepository subTodoRepository;

    @Override
    @Transactional
    public void updateSubTodoCompletionStatus(Long bifId, Long todoId, Long subTodoId, boolean isCompleted) {
        SubTodo subTodo = subTodoRepository.findById(subTodoId).orElseThrow(() -> new SubTodoNotFoundException(subTodoId));

        if (!subTodo.getTodo().getTodoId().equals(todoId)) {
            throw new InvalidSubTodoRelationException(subTodoId, todoId);
        }

        if (!subTodo.getTodo().getBifUser().getBifId().equals(bifId)) {
            throw new UnauthorizedSubTodoAccessException(subTodoId);
        }

        subTodo.setIsCompleted(isCompleted);
        if (isCompleted) {
            subTodo.setCompletedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        } else {
            subTodo.setCompletedAt(null);
        }
    }

    @Override
    @Transactional
    public void updateSubTodo(Long bifId, Long todoId, Long subTodoId, SubTodoUpdateRequest request) {
        SubTodo subTodo = subTodoRepository.findById(subTodoId).orElseThrow(() -> new SubTodoNotFoundException(subTodoId));

        if (!subTodo.getTodo().getTodoId().equals(todoId)) {
            throw new InvalidSubTodoRelationException(subTodoId, todoId);
        }

        if (!subTodo.getTodo().getBifUser().getBifId().equals(bifId)) {
            throw new UnauthorizedSubTodoAccessException(subTodoId);
        }

        subTodo.setTitle(request.getTitle());
        subTodo.setSortOrder(request.getSortOrder());
    }

}
