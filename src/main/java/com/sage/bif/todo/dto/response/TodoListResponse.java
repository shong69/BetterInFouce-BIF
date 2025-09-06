package com.sage.bif.todo.dto.response;

import com.sage.bif.todo.entity.Todo;
import com.sage.bif.todo.entity.enums.TodoTypes;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoListResponse {

    private Long todoId;
    private String title;
    private TodoTypes type;
    private Boolean hasOrder;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private Boolean isCompleted;
    private List<SubTodoInfo> subTodos;

    public static TodoListResponse from(Todo todo) {
        return from(todo, false);
    }

    public static TodoListResponse from(Todo todo, boolean isRoutineCompletedToday) {
        List<SubTodoInfo> subTodoInfos = Collections.emptyList();
        boolean hasOrder = false;

        if (todo.getSubTodos() != null) {
            subTodoInfos = todo.getSubTodos().stream()
                    .filter(subTodo -> !subTodo.getIsDeleted())
                    .map(subTodo -> SubTodoInfo.builder()
                            .subTodoId(subTodo.getSubTodoId())
                            .title(subTodo.getTitle())
                            .sortOrder(subTodo.getSortOrder())
                            .isCompleted(todo.getType() != TodoTypes.ROUTINE && subTodo.getIsCompleted())
                            .build())
                    .collect(Collectors.toList());

            hasOrder = !subTodoInfos.isEmpty() && subTodoInfos.stream().allMatch(subTodo -> subTodo.getSortOrder() > 0);
        }

        boolean isCompleted = todo.getType() == TodoTypes.ROUTINE
                ? isRoutineCompletedToday
                : todo.getIsCompleted();

        return TodoListResponse.builder()
                .todoId(todo.getTodoId())
                .title(todo.getTitle())
                .type(todo.getType())
                .hasOrder(hasOrder)
                .dueDate(todo.getDueDate())
                .dueTime(todo.getDueTime())
                .isCompleted(isCompleted)
                .subTodos(subTodoInfos)
                .build();
    }

    @Getter
    @Builder
    public static class SubTodoInfo {
        private Long subTodoId;
        private String title;
        private Integer sortOrder;
        private Boolean isCompleted;
    }

}
