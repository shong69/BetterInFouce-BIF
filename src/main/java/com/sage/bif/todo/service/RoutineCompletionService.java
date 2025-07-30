package com.sage.bif.todo.service;

import java.time.LocalDate;

public interface RoutineCompletionService {

    void completeRoutine(Long bifId, Long todoId, LocalDate completionDate);

    void uncompleteRoutine(Long bifId, Long todoId, LocalDate completionDate);

}
