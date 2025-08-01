package com.sage.bif.todo.service;

import com.sage.bif.todo.entity.RoutineCompletion;
import com.sage.bif.todo.entity.Todo;
import com.sage.bif.todo.repository.RoutineCompletionRepository;
import com.sage.bif.todo.repository.TodoRepository;
import com.sage.bif.todo.exception.TodoNotFoundException;
import com.sage.bif.todo.exception.UnauthorizedTodoAccessException;
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
    public void completeRoutine(Long bifId, Long todoId, LocalDate completionDate) {

        Todo todo = todoRepository.findById(todoId).orElseThrow(() -> new TodoNotFoundException(todoId));

        if (!todo.getBifId().getBifId().equals(bifId)) {
            throw new UnauthorizedTodoAccessException(bifId, todoId);
        }

        Optional<RoutineCompletion> existing = routineCompletionRepository.findByTodoIdAndCompletionDate(todo, completionDate);

        if (existing.isEmpty()) {
            RoutineCompletion completion = RoutineCompletion.builder()
                    .todoId(todo)
                    .completionDate(completionDate)
                    .build();
            routineCompletionRepository.save(completion);
        }

    }

    @Override
    @Transactional
    public void uncompleteRoutine(Long bifId, Long todoId, LocalDate completionDate) {

        Todo todo = todoRepository.findById(todoId).orElseThrow(() -> new TodoNotFoundException(todoId));

        if (!todo.getBifId().getBifId().equals(bifId)) {
            throw new UnauthorizedTodoAccessException(bifId, todoId);
        }

        routineCompletionRepository.deleteByTodoIdAndCompletionDate(todo, completionDate);

    }

}
