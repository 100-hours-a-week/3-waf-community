package com.ktb.community.controller;

import com.ktb.community.config.RateLimit;
import com.ktb.community.dto.ApiResponse;
import com.ktb.community.dto.request.LoginRequest;
import com.ktb.community.dto.request.RefreshTokenRequest;
import com.ktb.community.dto.request.SignupRequest;
import com.ktb.community.dto.response.AuthResponse;
import com.ktb.community.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
     * 로그인 (API.md Section 1.1)
     * POST /auth/login
     * Tier 1: 강한 제한 (brute-force 방지)
     * httpOnly Cookie 방식으로 토큰 전달 (XSS 방어)
     */
    @PostMapping("/login")
    @RateLimit(requestsPerMinute = 5)
    public ResponseEntity<ApiResponse<Void>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.login(request);

        // Access Token → httpOnly 쿠키
        Cookie accessCookie = new Cookie("access_token", authResponse.getAccessToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false);  // TODO: 운영 환경에서는 true (HTTPS)
        accessCookie.setPath("/");
        accessCookie.setMaxAge(30 * 60);  // 30분
        accessCookie.setAttribute("SameSite", "Strict");  // CSRF 방어
        response.addCookie(accessCookie);

        // Refresh Token → httpOnly 쿠키
        Cookie refreshCookie = new Cookie("refresh_token", authResponse.getRefreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);  // TODO: 운영 환경에서는 true (HTTPS)
        refreshCookie.setPath("/auth/refresh_token");  // refresh 엔드포인트만 전송
        refreshCookie.setMaxAge(7 * 24 * 60 * 60);  // 7일
        refreshCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(ApiResponse.success("login_success"));
    }
    
    /**
     * 로그아웃 (API.md Section 1.2)
     * POST /auth/logout
     * Cookie에서 Refresh Token 추출 및 삭제
     * Tier 3: 제한 없음 (공격 동인 없음)
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        // Refresh Token 추출 (Cookie)
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        // DB에서 삭제
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        // 쿠키 삭제 (MaxAge=0)
        Cookie accessCookie = new Cookie("access_token", null);
        accessCookie.setMaxAge(0);
        accessCookie.setPath("/");
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refresh_token", null);
        refreshCookie.setMaxAge(0);
        refreshCookie.setPath("/auth/refresh_token");
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(ApiResponse.success("logout_success"));
    }
    
    /**
     * 액세스 토큰 재발급 (API.md Section 1.3)
     * POST /auth/refresh_token
     * Cookie에서 Refresh Token 추출하여 새 Access Token 발급
     * Tier 2: 중간 제한 (비정상 토큰 갱신 감지)
     */
    @PostMapping("/refresh_token")
    @RateLimit(requestsPerMinute = 30)
    public ResponseEntity<ApiResponse<Void>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        // Refresh Token 추출 (Cookie)
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        // 새 Access Token 발급
        AuthResponse authResponse = authService.refreshAccessToken(refreshToken);

        // Access Token → httpOnly 쿠키 (Refresh Token은 그대로 유지)
        Cookie accessCookie = new Cookie("access_token", authResponse.getAccessToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false);  // TODO: 운영 환경에서는 true (HTTPS)
        accessCookie.setPath("/");
        accessCookie.setMaxAge(30 * 60);  // 30분
        accessCookie.setAttribute("SameSite", "Strict");
        response.addCookie(accessCookie);

        return ResponseEntity.ok(ApiResponse.success("token_refreshed"));
    }
}
