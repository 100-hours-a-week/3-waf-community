package com.ktb.community.controller;

import com.ktb.community.config.RateLimit;
import com.ktb.community.dto.ApiResponse;
import com.ktb.community.dto.request.ChangePasswordRequest;
import com.ktb.community.dto.request.SignupRequest;
import com.ktb.community.dto.request.UpdateProfileRequest;
import com.ktb.community.dto.response.AuthResponse;
import com.ktb.community.dto.response.UserResponse;
import com.ktb.community.enums.ErrorCode;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.service.AuthService;
import com.ktb.community.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 컨트롤러
 * API.md Section 2 참조
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    
    private final AuthService authService;
    private final UserService userService;
    
    /**
     * 회원가입 (API.md 2.1)
     * POST /users/signup or POST /users
     * Multipart 방식: 이미지와 데이터 함께 전송
     */
    @PostMapping(value = {"/signup", ""}, consumes = "multipart/form-data")
    @RateLimit(requestsPerMinute = 100)
    public ResponseEntity<ApiResponse<AuthResponse>> signup(
            @RequestPart("email") String email,
            @RequestPart("password") String password,
            @RequestPart("nickname") String nickname,
            @RequestPart(value = "profile_image", required = false) 
            org.springframework.web.multipart.MultipartFile profileImage) {
        
        // Manual Validation (P0 수정: Bean Validation 대체)
        
        // 1. Email 검증
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이메일은 필수입니다");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효한 이메일 형식이어야 합니다");
        }
        
        // 2. Password 검증
        if (password == null || password.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "비밀번호는 필수입니다");
        }
        if (password.length() < 8 || password.length() > 20) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "비밀번호는 8-20자여야 합니다");
        }
        
        // 3. Nickname 검증
        if (nickname == null || nickname.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "닉네임은 필수입니다");
        }
        if (nickname.length() > 10) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "닉네임은 최대 10자입니다");
        }
        
        // SignupRequest 생성
        SignupRequest request = SignupRequest.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .build();
        
        AuthResponse response = authService.signup(request, profileImage);
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
     * Multipart 방식: 이미지와 데이터 함께 전송
     */
    @PatchMapping(value = "/{userId}", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @PathVariable Long userId,
            @RequestPart(value = "nickname", required = false) String nickname,
            @RequestPart(value = "profile_image", required = false) 
            org.springframework.web.multipart.MultipartFile profileImage,
            Authentication authentication) {
        
        Long authenticatedUserId = extractUserIdFromAuthentication(authentication);
        
        // Manual Validation (P0 수정: Bean Validation 대체)
        if (nickname != null && nickname.length() > 10) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "닉네임은 최대 10자입니다");
        }
        
        // UpdateProfileRequest 생성
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .nickname(nickname)
                .build();
        
        UserResponse response = userService.updateProfile(userId, authenticatedUserId, request, profileImage);
        
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
     * JWT 인증: username = userId (숫자)
     * 기타 인증: username = email (fallback to DB lookup)
     */
    private Long extractUserIdFromAuthentication(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        
        // JWT 인증 경로: username이 userId인 경우 (빠른 경로)
        try {
            return Long.parseLong(username);
        } catch (NumberFormatException e) {
            // 테스트 환경 등: username이 email인 경우 (fallback)
            log.debug("Username is not numeric (likely email), falling back to DB lookup: {}", username);
            return userService.findUserIdByEmail(username);
        }
    }
}
