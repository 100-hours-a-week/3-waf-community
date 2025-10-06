package com.ktb.community.controller;

import com.ktb.community.dto.ApiResponse;
import com.ktb.community.dto.request.ChangePasswordRequest;
import com.ktb.community.dto.request.SignupRequest;
import com.ktb.community.dto.request.UpdateProfileRequest;
import com.ktb.community.dto.response.AuthResponse;
import com.ktb.community.dto.response.UserResponse;
import com.ktb.community.service.AuthService;
import com.ktb.community.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 컨트롤러
 * API.md Section 2 참조
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    
    private final AuthService authService;
    private final UserService userService;
    
    /**
     * 회원가입 (API.md 2.1)
     * POST /users/signup or POST /users
     */
    @PostMapping({"/signup", ""})
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("register_success", response));
    }
    
    /**
     * 사용자 정보 조회 (API.md 2.2)
     * GET /users/{userID}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@PathVariable Long userId) {
        UserResponse response = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("get_profile_success", response));
    }
    
    /**
     * 사용자 정보 수정 (API.md 2.3)
     * PATCH /users/{userID}
     */
    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        
        Long authenticatedUserId = extractUserIdFromAuthentication(authentication);
        UserResponse response = userService.updateProfile(userId, authenticatedUserId, request);
        
        return ResponseEntity.ok(ApiResponse.success("update_profile_success", response));
    }
    
    /**
     * 비밀번호 변경 (API.md 2.4)
     * PATCH /users/{userID}/password
     */
    @PatchMapping("/{userId}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long userId,
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        
        Long authenticatedUserId = extractUserIdFromAuthentication(authentication);
        userService.changePassword(userId, authenticatedUserId, request);
        
        return ResponseEntity.ok(ApiResponse.success("update_password_success"));
    }
    
    /**
     * 회원 탈퇴 (API.md 2.5)
     * PUT /users/{userID}
     */
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(
            @PathVariable Long userId,
            Authentication authentication) {
        
        Long authenticatedUserId = extractUserIdFromAuthentication(authentication);
        userService.deactivateAccount(userId, authenticatedUserId);
        
        return ResponseEntity.ok(ApiResponse.success("account_deactivated_success"));
    }
    
    /**
     * Authentication에서 사용자 ID 추출
     * CustomUserDetailsService.loadUserById()로 설정된 UserDetails의 username은 email
     */
    private Long extractUserIdFromAuthentication(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        
        // CustomUserDetailsService에서 email을 username으로 설정했으므로
        // UserRepository를 통해 userId를 조회
        // TODO: 성능 개선 - JWT에 userId를 claim으로 추가하고 직접 추출하는 방식 고려
        return userService.findUserIdByEmail(email);
    }
}
