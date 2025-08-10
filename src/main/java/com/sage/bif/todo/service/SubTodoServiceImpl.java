package com.sage.bif.todo.service;

import com.sage.bif.todo.dto.request.SubTodoUpdateRequest;
import com.sage.bif.todo.entity.SubTodo;
import com.sage.bif.todo.event.model.SubTodoCompletedEvent;
import com.sage.bif.todo.exception.InvalidSubTodoRelationException;
import com.sage.bif.todo.exception.SubTodoNotFoundException;
import com.sage.bif.todo.exception.UnauthorizedSubTodoAccessException;
import com.sage.bif.todo.repository.SubTodoRepository;
import com.sage.bif.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubTodoServiceImpl implements SubTodoService {

    private final SubTodoRepository subTodoRepository;
    private final TodoRepository todoRepository;
    private final ApplicationEventPublisher eventPublisher;

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
            subTodo.setCompletedAt(LocalDateTime.now());

            publishSubTodoCompletedEvent(subTodo, bifId, todoId);
        } else {
            subTodo.setCompletedAt(null);
        }
    }

    private void publishSubTodoCompletedEvent(SubTodo subTodo, Long bifId, Long todoId) {
        List<SubTodo> allSubTodos = subTodoRepository.findByTodo_TodoIdAndIsDeletedFalse(todoId);
        int completedCount = (int) allSubTodos.stream()
                .filter(SubTodo::getIsCompleted)
                .count();
        int totalCount = allSubTodos.size();

        boolean isParentTodoCompleted = (completedCount == totalCount);

        SubTodoCompletedEvent event = new SubTodoCompletedEvent(
                this,
                subTodo,
                bifId,
                todoId,
                isParentTodoCompleted,
                completedCount,
                totalCount
        );
        eventPublisher.publishEvent(event);
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
