package com.sage.bif.todo.dto.request;

import com.sage.bif.todo.entity.enums.RepeatDays;
import com.sage.bif.todo.entity.enums.RepeatFrequency;
import com.sage.bif.todo.entity.enums.TodoTypes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoUpdateRequest {

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotNull(message = "Todo 타입은 필수입니다")
    private TodoTypes type;

    private RepeatFrequency repeatFrequency;
    private List<RepeatDays> repeatDays;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private Boolean notificationEnabled;
    private Integer notificationTime;

    @Valid
    private List<SubTodoUpdateRequest> subTodos;

}
