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

import java.sql.Date;
import java.sql.Time;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoUpdateRequest {

    @NotBlank(message = "할일 내용은 필수입니다.")
    private String userInput;

    @NotBlank(message = "할일 제목은 필수입니다.")
    private String title;

    @NotNull(message = "할일 유형은 필수입니다.")
    private TodoTypes type;

    private RepeatFrequency repeatFrequency;
    private List<RepeatDays> repeatDays;

    private Date dueDate;
    private Time dueTime;

    private Boolean notificationEnabled;
    private Integer notificationTime;

    @Valid
    private List<SubTodoCreateRequest> subTodos;
}
