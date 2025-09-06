package com.sage.bif.todo.service;

import com.sage.bif.todo.dto.request.SubTodoUpdateRequest;
import com.sage.bif.todo.entity.SubTodo;
import com.sage.bif.todo.entity.enums.TodoTypes;
import com.sage.bif.todo.exception.InvalidSubTodoRelationException;
import com.sage.bif.todo.exception.SubTodoNotFoundException;
import com.sage.bif.todo.exception.UnauthorizedSubTodoAccessException;
import com.sage.bif.todo.repository.SubTodoCompletionRepository;
import com.sage.bif.todo.repository.SubTodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class SubTodoServiceImpl implements SubTodoService {

    private static final String TIMEZONE_ASIA_SEOUL = "Asia/Seoul";

    private final SubTodoRepository subTodoRepository;
    private final SubTodoCompletionRepository subTodoCompletionRepository;

    @Override
    @Transactional
    public void updateSubTodoCompletionStatus(Long bifId, Long todoId, Long subTodoId, boolean isCompleted) {
        LocalDate completionDate = LocalDate.now(ZoneId.of(TIMEZONE_ASIA_SEOUL));
        updateSubTodoCompletionStatusInternal(bifId, todoId, subTodoId, isCompleted, completionDate);
    }

    @Override
    @Transactional
    public void updateSubTodoCompletionStatus(Long bifId, Long todoId, Long subTodoId, boolean isCompleted, LocalDate completionDate) {
        updateSubTodoCompletionStatusInternal(bifId, todoId, subTodoId, isCompleted, completionDate);
    }

    private void updateSubTodoCompletionStatusInternal(Long bifId, Long todoId, Long subTodoId, boolean isCompleted, LocalDate completionDate) {
        SubTodo subTodo = validateAndGetSubTodo(bifId, todoId, subTodoId);

        if (subTodo.getTodo().getType() == TodoTypes.ROUTINE) {
            if (isCompleted) {
                subTodoCompletionRepository.insertIgnoreCompletion(subTodoId, completionDate);
            } else {
                subTodoCompletionRepository.deleteBySubTodo_SubTodoIdAndCompletionDate(subTodoId, completionDate);
            }
        } else {
            subTodo.setIsCompleted(isCompleted);
            if (isCompleted) {
                subTodo.setCompletedAt(LocalDateTime.now(ZoneId.of(TIMEZONE_ASIA_SEOUL)));
            } else {
                subTodo.setCompletedAt(null);
            }
        }
    }

    @Override
    @Transactional
    public void updateSubTodo(Long bifId, Long todoId, Long subTodoId, SubTodoUpdateRequest request) {
        SubTodo subTodo = validateAndGetSubTodo(bifId, todoId, subTodoId);

        subTodo.setTitle(request.getTitle());
        subTodo.setSortOrder(request.getSortOrder());
    }

    private SubTodo validateAndGetSubTodo(Long bifId, Long todoId, Long subTodoId) {
        SubTodo subTodo = subTodoRepository.findById(subTodoId).orElseThrow(() -> new SubTodoNotFoundException(subTodoId));

        if (!subTodo.getTodo().getTodoId().equals(todoId)) {
            throw new InvalidSubTodoRelationException(subTodoId, todoId);
        }

        if (!subTodo.getTodo().getBifUser().getBifId().equals(bifId)) {
            throw new UnauthorizedSubTodoAccessException(subTodoId);
        }

        return subTodo;
    }

}
