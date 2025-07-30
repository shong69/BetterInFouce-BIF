package com.sage.bif.user.controller;

// import com.sage.bif.common.dto.ApiResponse;
// import com.sage.bif.common.exception.BaseException;
// import com.sage.bif.common.exception.ErrorCode;
// import com.sage.bif.user.dto.request.UserCreateRequest;
// import com.sage.bif.user.dto.response.UserResponse;
// import io.swagger.v3.oas.annotations.Operation;
// import io.swagger.v3.oas.annotations.Parameter;
// import io.swagger.v3.oas.annotations.media.Content;
// import io.swagger.v3.oas.annotations.media.Schema;
// import io.swagger.v3.oas.annotations.tags.Tag;
// import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users")
// @Tag(name = "User Management", description = "사용자 관리 API")
public class UserController {
    
    // @GetMapping("/{id}")
    // @Operation(summary = "사용자 조회", description = "ID로 사용자 정보를 조회합니다. (200: 성공, 400: 잘못된 요청/사용자 없음, 500: 서버 오류)")
    // public ResponseEntity<ApiResponse<UserResponse>> getUser(
    //     @Parameter(description = "사용자 ID", required = true) @PathVariable Long id) {
        
    //     // 실제 예외 처리가 작동하는 시나리오
    //     if (id <= 0) {
    //         log.info("BaseException 발생: INVALID_INPUT");
    //         throw new BaseException(ErrorCode.INVALID_INPUT, "사용자 ID는 0보다 커야 합니다. 입력된 ID: " + id);
    //     }
        
    //     // 존재하지 않는 사용자 ID로 테스트 (ID가 999인 경우)
    //     if (id == 999) {
    //         log.info("BaseException 발생: USER_NOT_FOUND");
    //         throw new BaseException(ErrorCode.USER_NOT_FOUND, "사용자 ID " + id + "를 찾을 수 없습니다");
    //     }
        
    //     // 서버 오류 시뮬레이션 (ID가 500인 경우)
    //     if (id == 500) {
    //         log.info("RuntimeException 발생: 데이터베이스 연결 오류");
    //         throw new RuntimeException("데이터베이스 연결 오류");
    //     }
        
    //     // 정상적인 경우
    //     UserResponse user = new UserResponse();
    //     user.setId(id);
    //     user.setName("테스트 사용자 " + id);
    //     user.setEmail("user" + id + "@example.com");
        
    //     return ResponseEntity.ok(ApiResponse.success(user, "사용자 조회 성공"));
    // }
    
    // @PostMapping
    // @Operation(summary = "사용자 등록", description = "새로운 사용자를 등록합니다. (201: 성공, 400: 잘못된 요청/중복 사용자)")
    // public ResponseEntity<ApiResponse<UserResponse>> createUser(
    //     @Parameter(description = "사용자 정보", required = true) 
    //     @Valid @RequestBody UserCreateRequest userRequest) {
        
    //     // 중복 이메일 체크 시뮬레이션
    //     if ("duplicate@example.com".equals(userRequest.getEmail())) {
    //         throw new BaseException(ErrorCode.USER_ALREADY_EXISTS, "이미 존재하는 이메일입니다: " + userRequest.getEmail());
    //     }
        
    //     // 정상적인 경우
    //     UserResponse createdUser = UserResponse.builder()
    //         .id(1L)
    //         .name(userRequest.getName())
    //         .email(userRequest.getEmail())
    //         .build();
            
    //     return ResponseEntity.status(201)
    //         .body(ApiResponse.success(createdUser, "사용자 등록 성공"));
    // }
    
    // @GetMapping
    // @Operation(summary = "사용자 목록 조회", description = "모든 사용자 목록을 조회합니다. (200: 성공, 400: 권한 없음)")
    // public ResponseEntity<ApiResponse<java.util.List<UserResponse>>> getAllUsers() {
        
    //     // 권한 체크 시뮬레이션 (관리자만 접근 가능)
    //     // 실제로는 SecurityContext에서 사용자 권한을 확인
    //     boolean isAdmin = false; // 테스트용
        
    //     if (!isAdmin) {
    //         throw new BaseException(ErrorCode.FORBIDDEN, "사용자 목록 조회 권한이 없습니다");
    //     }
        
    //     // 정상적인 경우
    //     java.util.List<UserResponse> users = java.util.List.of(
    //         UserResponse.builder().id(1L).name("사용자1").email("user1@example.com").build(),
    //         UserResponse.builder().id(2L).name("사용자2").email("user2@example.com").build()
    //     );
        
    //     return ResponseEntity.ok(ApiResponse.success(users, "사용자 목록 조회 성공"));
    // }
} 