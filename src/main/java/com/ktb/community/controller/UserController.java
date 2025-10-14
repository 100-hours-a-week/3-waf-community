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
import com.ktb.community.util.PasswordValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ModelAttribute;

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
            @Valid @ModelAttribute SignupRequest request) {
        
        // 비밀번호 정책 검증 (Bean Validation 외 추가 정책)
        if (!PasswordValidator.isValid(request.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_POLICY,
                    PasswordValidator.getPolicyDescription());
        }
        
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
     * Multipart 방식: 이미지와 데이터 함께 전송
     */
    @PatchMapping(value = "/{userId}", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @PathVariable Long userId,
            @Valid @ModelAttribute UpdateProfileRequest request,
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
        
        // 1. 비밀번호 일치 검증
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH,
                    "Password confirmation does not match");
        }
        
        // 2. 비밀번호 정책 검증
        if (!PasswordValidator.isValid(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_POLICY,
                    PasswordValidator.getPolicyDescription());
        }
        
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
