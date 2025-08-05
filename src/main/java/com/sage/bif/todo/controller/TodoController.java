package com.sage.bif.todo.controller;

import com.sage.bif.common.dto.CustomUserDetails;
import com.sage.bif.todo.dto.request.AiTodoCreateRequest;
import com.sage.bif.todo.dto.request.SubTodoCompletionUpdateRequest;
import com.sage.bif.todo.dto.request.SubTodoUpdateRequest;
import com.sage.bif.todo.dto.request.TodoUpdateRequest;
import com.sage.bif.todo.dto.response.TodoListResponse;
import com.sage.bif.todo.dto.response.TodoUpdatePageResponse;
import com.sage.bif.todo.service.SubTodoService;
import com.sage.bif.todo.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/todos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Todo Management", description = "할 일 관리 API")
public class TodoController {

    private final TodoService todoService;
    private final SubTodoService subTodoService;

    @PostMapping
    @Operation(summary = "AI로 할 일 생성")
    public ResponseEntity<TodoListResponse> createTodoByAi(
            @Parameter(description = "할 일 생성 요청", required = true)
            @Valid @RequestBody AiTodoCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long bifId = userDetails.getBifId();
        TodoListResponse response = todoService.createTodoByAi(bifId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "할 일 목록 조회")
    public ResponseEntity<List<TodoListResponse>> getTodoList(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long bifId = customUserDetails.getBifId();
        LocalDate targetDate = date != null ? date : LocalDate.now();

        List<TodoListResponse> response = todoService.getTodoList(bifId, targetDate);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{todoId}")
    @Operation(summary = "상세 할 일 목록 조회")
    public ResponseEntity<TodoUpdatePageResponse> getTodoDetail(
            @PathVariable Long todoId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long bifId = customUserDetails.getBifId();

        TodoUpdatePageResponse response = todoService.getTodoDetail(bifId, todoId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{todoId}")
    @Operation(summary = "할 일 목록 수정")
    public ResponseEntity<TodoListResponse> updateTodo(
            @PathVariable Long todoId,
            @Valid @RequestBody TodoUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long bifId = customUserDetails.getBifId();
        TodoListResponse response = todoService.updateTodo(bifId, todoId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{todoId}")
    @Operation(summary = "할 일 목록 삭제")
    public ResponseEntity<Void> deleteTodo(
            @PathVariable Long todoId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long bifId = customUserDetails.getBifId();
        todoService.deleteTodo(bifId, todoId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{todoId}/complete")
    @Operation(summary = "할 일 완료 상태 변경")
    public ResponseEntity<TodoListResponse> completeTodo(
            @PathVariable Long todoId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long bifId = customUserDetails.getBifId();
        TodoListResponse response = todoService.completeTodo(bifId, todoId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{todoId}/uncomplete")
    @Operation(summary = "할 일 미완료 상태 변경")
    public ResponseEntity<TodoListResponse> uncompleteTodo(
            @PathVariable Long todoId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long bifId = customUserDetails.getBifId();
        TodoListResponse response = todoService.uncompleteTodo(bifId, todoId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{todoId}/subtodos/{subTodoId}/complete")
    @Operation(summary = "세부 할 일 완료 상태 변경")
    public ResponseEntity<Void> updateSubTodoCompletionStatus(
            @PathVariable Long todoId,
            @PathVariable Long subTodoId,
            @Valid @RequestBody SubTodoCompletionUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long bifId = customUserDetails.getBifId();
        subTodoService.updateSubTodoCompletionStatus(bifId, todoId, subTodoId, request.getIsCompleted());

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{todoId}/subtodos/{subTodoId}")
    @Operation(summary = "세부 할 일 수정")
    public ResponseEntity<Void> updateSubTodo(
            @PathVariable Long todoId,
            @PathVariable Long subTodoId,
            @Valid @RequestBody SubTodoUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long bifId = customUserDetails.getBifId();
        subTodoService.updateSubTodo(bifId, todoId, subTodoId, request);

        return ResponseEntity.noContent().build();
    }

}