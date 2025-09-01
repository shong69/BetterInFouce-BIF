package com.sage.bif.todo.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sage.bif.todo.entity.Todo;
import com.sage.bif.todo.entity.enums.RepeatDays;
import com.sage.bif.todo.entity.enums.RepeatFrequency;
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
public class TodoUpdatePageResponse {

    private Long todoId;
    private String title;
    private TodoTypes type;
    private Boolean hasOrder;
    private RepeatFrequency repeatFrequency;
    private List<RepeatDays> repeatDays;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime dueTime;
    private Boolean notificationEnabled;
    private Integer notificationTime;
    private Boolean isCompleted;
    private List<SubTodoInfo> subTodos;
    private Integer currentStep;

    public static TodoUpdatePageResponse from(Todo todo) {
        List<SubTodoInfo> subTodoInfos = Collections.emptyList();
        boolean hasOrder = false;

        if (todo.getSubTodos() != null) {
            subTodoInfos = todo.getSubTodos().stream()
                    .filter(subTodo -> !subTodo.getIsDeleted())
                    .map(subTodo -> SubTodoInfo.builder()
                            .subTodoId(subTodo.getSubTodoId())
                            .title(subTodo.getTitle())
                            .sortOrder(subTodo.getSortOrder())
                            .isCompleted(subTodo.getIsCompleted())
                            .build())
                    .collect(Collectors.toList());

            hasOrder = !subTodoInfos.isEmpty() && subTodoInfos.stream().allMatch(subTodo -> subTodo.getSortOrder() > 0);
        }

        return TodoUpdatePageResponse.builder()
                .todoId(todo.getTodoId())
                .title(todo.getTitle())
                .type(todo.getType())
                .hasOrder(hasOrder)
                .repeatFrequency(todo.getRepeatFrequency())
                .repeatDays(todo.getRepeatDays())
                .dueDate(todo.getDueDate())
                .dueTime(todo.getDueTime())
                .notificationEnabled(todo.getNotificationEnabled())
                .notificationTime(todo.getNotificationTime())
                .isCompleted(todo.getIsCompleted())
                .subTodos(subTodoInfos)
                .currentStep(todo.getCurrentStep())
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
