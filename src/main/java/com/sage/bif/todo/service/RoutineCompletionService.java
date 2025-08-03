package com.sage.bif.todo.service;

import java.time.LocalDate;

public interface RoutineCompletionService {

    boolean completeRoutine(Long bifId, Long todoId, LocalDate completionDate);

    boolean uncompleteRoutine(Long bifId, Long todoId, LocalDate completionDate);

}
