package com.sage.bif.todo.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TodoCompletionRequest {

    @NotNull
    private Boolean isCompleted;

}
