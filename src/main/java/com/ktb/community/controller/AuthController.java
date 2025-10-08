package com.ktb.community.controller;

import com.ktb.community.config.RateLimit;
import com.ktb.community.dto.ApiResponse;
import com.ktb.community.dto.request.LoginRequest;
import com.ktb.community.dto.request.RefreshTokenRequest;
import com.ktb.community.dto.request.SignupRequest;
import com.ktb.community.dto.response.AuthResponse;
import com.ktb.community.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 컨트롤러
 * API.md Section 1 참조
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 로그인 (API.md 1.1)
     * POST /auth/login
     */
    @PostMapping("/login")
    @RateLimit(requestsPerMinute = 100)
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("login_success", response));
    }
    
    /**
     * 로그아웃 (API.md 1.2)
     * POST /auth/logout
     */
    @PostMapping("/logout")
    @RateLimit(requestsPerMinute = 100)
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("logout_success"));
    }
    
    /**
     * 액세스 토큰 재발급 (API.md 1.3)
     * POST /auth/refresh_token
     */
    @PostMapping("/refresh_token")
    @RateLimit(requestsPerMinute = 100)
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("token_refreshed", response));
    }
}
