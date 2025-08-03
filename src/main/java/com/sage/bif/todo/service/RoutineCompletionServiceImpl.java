package com.sage.bif.todo.service;

import com.sage.bif.todo.entity.RoutineCompletion;
import com.sage.bif.todo.entity.Todo;
import com.sage.bif.todo.entity.enums.TodoTypes;
import com.sage.bif.todo.exception.TodoNotFoundException;
import com.sage.bif.todo.exception.UnauthorizedTodoAccessException;
import com.sage.bif.todo.repository.RoutineCompletionRepository;
import com.sage.bif.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoutineCompletionServiceImpl implements RoutineCompletionService {

    private final TodoRepository todoRepository;
    private final RoutineCompletionRepository routineCompletionRepository;

    @Override
    @Transactional
    public boolean completeRoutine(Long bifId, Long todoId, LocalDate completionDate) {
        Todo todo = validateRoutineAccess(bifId, todoId);

        Optional<RoutineCompletion> existing = routineCompletionRepository
                .findByTodo_TodoIdAndCompletionDate(todoId, completionDate);

        if (existing.isPresent()) {
            return false;
        }

        RoutineCompletion completion = RoutineCompletion.builder()
                .todo(todo)
                .completionDate(completionDate)
                .build();

        routineCompletionRepository.save(completion);

        return true;
    }

    @Override
    @Transactional
    public boolean uncompleteRoutine(Long bifId, Long todoId, LocalDate completionDate) {
        validateRoutineAccess(bifId, todoId);

        int deletedCount = routineCompletionRepository.deleteByTodo_TodoIdAndCompletionDate(todoId, completionDate);

        return deletedCount > 0;
    }

    private Todo validateRoutineAccess(Long bifId, Long todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new TodoNotFoundException(todoId));

        if (!todo.getBifUser().getBifId().equals(bifId)) {
            throw new UnauthorizedTodoAccessException(todoId);
        }

        if (Boolean.TRUE.equals(todo.getIsDeleted())) {
            throw new TodoNotFoundException(todoId);
        }

        if (todo.getType() != TodoTypes.ROUTINE) {
            throw new IllegalArgumentException("루틴 타입의 할일만 루틴 완료 처리가 가능합니다.");
        }

        return todo;
    }
}
