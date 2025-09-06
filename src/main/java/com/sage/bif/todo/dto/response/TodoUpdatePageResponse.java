package com.sage.bif.todo.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sage.bif.todo.entity.SubTodoCompletion;
import com.sage.bif.todo.entity.Todo;
import com.sage.bif.todo.entity.enums.RepeatDays;
import com.sage.bif.todo.entity.enums.RepeatFrequency;
import com.sage.bif.todo.entity.enums.TodoTypes;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
        return from(todo, todo.getIsCompleted());
    }

    public static TodoUpdatePageResponse from(Todo todo, boolean isCompleted) {
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
                .isCompleted(isCompleted)
                .subTodos(subTodoInfos)
                .currentStep(todo.getCurrentStep())
                .build();
    }

    public static TodoUpdatePageResponse from(Todo todo, boolean isCompleted, List<SubTodoCompletion> subTodoCompletions) {
        List<SubTodoInfo> subTodoInfos = Collections.emptyList();
        boolean hasOrder = false;

        if (todo.getSubTodos() != null) {
            Map<Long, Boolean> completionMap = subTodoCompletions.stream()
                    .collect(Collectors.toMap(
                            completion -> completion.getSubTodo().getSubTodoId(),
                            completion -> true
                    ));

            subTodoInfos = todo.getSubTodos().stream()
                    .filter(subTodo -> !subTodo.getIsDeleted())
                    .map(subTodo -> {
                        boolean isSubTodoCompleted;
                        if (todo.getType() == TodoTypes.ROUTINE) {
                            isSubTodoCompleted = completionMap.getOrDefault(subTodo.getSubTodoId(), false);
                        } else {
                            isSubTodoCompleted = subTodo.getIsCompleted();
                        }

                        return SubTodoInfo.builder()
                                .subTodoId(subTodo.getSubTodoId())
                                .title(subTodo.getTitle())
                                .sortOrder(subTodo.getSortOrder())
                                .isCompleted(isSubTodoCompleted)
                                .build();
                    })
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
                .isCompleted(isCompleted)
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
