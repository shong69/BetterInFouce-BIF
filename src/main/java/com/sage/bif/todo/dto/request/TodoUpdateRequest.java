package com.sage.bif.todo.dto.request;

import com.sage.bif.todo.entity.enums.RepeatDays;
import com.sage.bif.todo.entity.enums.RepeatFrequency;
import com.sage.bif.todo.entity.enums.TodoTypes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
    @Size(min = 1, max = 255, message = "제목은 1-255자 사이여야 합니다")
    private String title;

    @NotNull(message = "Todo 타입은 필수입니다")
    private TodoTypes type;

    private RepeatFrequency repeatFrequency;

    @Size(max = 7, message = "반복 요일은 최대 7개까지 가능합니다")
    private List<RepeatDays> repeatDays;

    @FutureOrPresent(message = "마감일은 현재보다 미래여야 합니다")
    private LocalDate dueDate;

    private LocalTime dueTime;

    private Boolean notificationEnabled;

    @Min(value = 0, message = "알림 시간은 0분 이상이어야 합니다")
    @Max(value = 1440, message = "알림 시간은 1440분(24시간) 이하여야 합니다")
    private Integer notificationTime;

    @Valid
    @Size(max = 5, message = "세부 할일은 최대 5개까지 가능합니다")
    private List<SubTodoUpdateRequest> subTodos;

}
