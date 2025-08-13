package com.sage.bif.todo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubTodoUpdateRequest {

    private Long subTodoId;

    @NotBlank(message = "상세 할일 제목은 필수입니다")
    private String title;

    @NotNull(message = "정렬 순서는 필수입니다")
    private Integer sortOrder;

}
