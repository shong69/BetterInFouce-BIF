package com.sage.bif.todo.controller;

import com.sage.bif.common.dto.ApiResponse;
import com.sage.bif.common.dto.CustomUserDetails;
import com.sage.bif.common.jwt.JwtTokenProvider;
import com.sage.bif.todo.dto.request.AiTodoCreateRequest;
import com.sage.bif.todo.dto.request.SubTodoCompletionUpdateRequest;
import com.sage.bif.todo.dto.request.SubTodoUpdateRequest;
import com.sage.bif.todo.dto.request.TodoCompletionRequest;
import com.sage.bif.todo.dto.request.TodoUpdateRequest;
import com.sage.bif.todo.dto.response.TodoListResponse;
import com.sage.bif.todo.dto.response.TodoUpdatePageResponse;
import com.sage.bif.todo.exception.GuardianAccessDeniedException;
import com.sage.bif.todo.exception.GuardianConnectionNotFoundException;
import com.sage.bif.todo.exception.UnsupportedUserRoleException;
import com.sage.bif.todo.service.SubTodoService;
import com.sage.bif.todo.service.TodoService;
import com.sage.bif.user.repository.GuardianRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/todos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Todo Management", description = "할 일 관리 API")
public class TodoController {

    private static final String OPERATION_CREATE = "CREATE";
    private static final String OPERATION_DELETE = "DELETE";
    private static final String OPERATION_COMPLETE = "COMPLETE";
    private static final String TIMEZONE_ASIA_SEOUL = "Asia/Seoul";

    private final TodoService todoService;
    private final SubTodoService subTodoService;
    private final GuardianRepository guardianRepository;

    @PostMapping
    @Operation(summary = "AI로 할 일 생성")
    public ResponseEntity<TodoListResponse> createTodoByAi(
            @Parameter(description = "할 일 생성 요청", required = true)
            @Valid @RequestBody AiTodoCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails.getRole() == JwtTokenProvider.UserRole.GUARDIAN) {
            throw new GuardianAccessDeniedException("Guardian은 할 일을 생성할 수 없습니다.");
        }
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
        Long bifId = getBifIdForUser(customUserDetails);
        LocalDate targetDate = date != null ? date : LocalDate.now(ZoneId.of(TIMEZONE_ASIA_SEOUL));

        List<TodoListResponse> response = todoService.getTodoList(bifId, targetDate);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{todoId}")
    @Operation(summary = "상세 할 일 목록 조회")
    public ResponseEntity<TodoUpdatePageResponse> getTodoDetail(
            @PathVariable Long todoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long bifId = getBifIdForUser(customUserDetails);
        LocalDate viewDate = date != null ? date : LocalDate.now(ZoneId.of(TIMEZONE_ASIA_SEOUL));

        TodoUpdatePageResponse response = todoService.getTodoDetail(bifId, todoId, viewDate);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{todoId}/step")
    @Operation(summary = "할 일 단계 업데이트")
    public ResponseEntity<ApiResponse<Void>> updateTodoStep(
            @PathVariable Long todoId,
            @RequestBody UpdateStepRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long bifId = getBifIdForUser(customUserDetails);
        todoService.updateCurrentStep(bifId, todoId, request.getStep());

        return ResponseEntity.ok(ApiResponse.success(null, "단계가 업데이트되었습니다."));
    }

    @PutMapping("/{todoId}")
    @Operation(summary = "할 일 목록 수정")
    public ResponseEntity<TodoUpdatePageResponse> updateTodo(
            @PathVariable Long todoId,
            @Valid @RequestBody TodoUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long bifId = getBifIdForUser(customUserDetails);
        TodoUpdatePageResponse response = todoService.updateTodo(bifId, todoId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{todoId}")
    @Operation(summary = "할 일 목록 삭제")
    public ResponseEntity<Void> deleteTodo(
            @PathVariable Long todoId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (customUserDetails.getRole() == JwtTokenProvider.UserRole.GUARDIAN) {
            throw new GuardianAccessDeniedException("Guardian은 할 일을 삭제할 수 없습니다.");
        }
        Long bifId = customUserDetails.getBifId();
        todoService.deleteTodo(bifId, todoId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{todoId}/completion")
    @Operation(summary = "할 일 완료/미완료 상태 변경")
    public ResponseEntity<TodoListResponse> updateTodoCompletion(
            @PathVariable Long todoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody TodoCompletionRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (customUserDetails.getRole() == JwtTokenProvider.UserRole.GUARDIAN) {
            throw new GuardianAccessDeniedException("Guardian은 할 일을 완료/미완료할 수 없습니다.");
        }
        Long bifId = customUserDetails.getBifId();
        LocalDate targetDate = date != null ? date : LocalDate.now(ZoneId.of(TIMEZONE_ASIA_SEOUL));

        TodoListResponse response = todoService.updateTodoCompletion(bifId, todoId, targetDate, request.getIsCompleted());

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{todoId}/subtodos/{subTodoId}/complete")
    @Operation(summary = "세부 할 일 완료 상태 변경")
    public ResponseEntity<Void> updateSubTodoCompletionStatus(
            @PathVariable Long todoId,
            @PathVariable Long subTodoId,
            @Valid @RequestBody SubTodoCompletionUpdateRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (customUserDetails.getRole() == JwtTokenProvider.UserRole.GUARDIAN) {
            throw new GuardianAccessDeniedException("Guardian은 할 일을 완료/미완료할 수 없습니다.");
        }
        Long bifId = customUserDetails.getBifId();

        if (date != null) {
            subTodoService.updateSubTodoCompletionStatus(bifId, todoId, subTodoId, request.getIsCompleted(), date);
        } else {
            subTodoService.updateSubTodoCompletionStatus(bifId, todoId, subTodoId, request.getIsCompleted());
        }

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{todoId}/subtodos/{subTodoId}")
    @Operation(summary = "세부 할 일 수정")
    public ResponseEntity<Void> updateSubTodo(
            @PathVariable Long todoId,
            @PathVariable Long subTodoId,
            @Valid @RequestBody SubTodoUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long bifId = getBifIdForUser(customUserDetails);
        subTodoService.updateSubTodo(bifId, todoId, subTodoId, request);

        return ResponseEntity.noContent().build();
    }

    private Long getBifIdForUser(CustomUserDetails userDetails) {
        if (userDetails.getRole() == JwtTokenProvider.UserRole.BIF) {
            return userDetails.getBifId();
        }
        if (userDetails.getRole() == JwtTokenProvider.UserRole.GUARDIAN) {
            return guardianRepository.findBifIdBySocialId(userDetails.getSocialId())
                    .orElseThrow(() -> new GuardianConnectionNotFoundException("Guardian과 연결된 BIF를 찾을 수 없습니다."));
        }
        throw new UnsupportedUserRoleException("지원하지 않는 사용자 역할입니다.");
    }

    @Getter
    @Setter
    public static class UpdateStepRequest {
        private int step;
    }

}
