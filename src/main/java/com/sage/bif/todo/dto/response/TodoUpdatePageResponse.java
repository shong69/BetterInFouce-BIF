package com.sage.bif.todo.dto.response;

import com.sage.bif.todo.entity.enums.RepeatDays;
import com.sage.bif.todo.entity.enums.RepeatFrequency;
import com.sage.bif.todo.entity.enums.TodoTypes;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoUpdatePageResponse {

    private Long todoId;
    private String title;
    private TodoTypes type;
    private RepeatFrequency repeatFrequency;
    private List<RepeatDays> repeatDays;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private Boolean notificationEnabled;
    private Integer notificationTime;
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
