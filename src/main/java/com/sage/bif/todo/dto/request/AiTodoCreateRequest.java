package com.sage.bif.todo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTodoCreateRequest {

    @NotBlank(message = "사용자 입력은 필수입니다")
    @Size(min = 1, max = 1000, message = "사용자 입력은 1-1000자 사이여야 합니다")
    private String userInput;

}
