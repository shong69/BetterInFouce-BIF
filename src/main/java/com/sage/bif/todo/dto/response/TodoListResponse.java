package com.sage.bif.todo.dto.response;

import com.sage.bif.todo.entity.enums.TodoTypes;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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

    @Getter
    @Builder
    public static class SubTodoInfo {
        private Long subTodoId;
        private String title;
        private Integer sortOrder;
        private boolean isCompleted;
    }

}
