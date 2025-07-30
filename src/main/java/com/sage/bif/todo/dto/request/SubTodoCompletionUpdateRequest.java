package com.sage.bif.todo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubTodoCompletionUpdateRequest {

    private Long subTodoId;
    private Boolean isCompleted;

}
