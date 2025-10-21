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
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthService.AuthResult result = authService.login(request);

        // 토큰 → httpOnly Cookie 설정
        setCookie(response, "access_token", result.tokens().getAccessToken(), 30 * 60, "/");
        setCookie(response, "refresh_token", result.tokens().getRefreshToken(), 7 * 24 * 60 * 60, "/auth/refresh_token");

        // 사용자 정보 → 응답 body
        AuthResponse authResponse = AuthResponse.from(result.user());
        return ResponseEntity.ok(ApiResponse.success("login_success", authResponse));
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
     * 사용자 정보도 함께 반환 (프론트엔드 localStorage 동기화용)
     * Tier 2: 중간 제한 (비정상 토큰 갱신 감지)
     */
    @PostMapping("/refresh_token")
    @RateLimit(requestsPerMinute = 30)
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        // Refresh Token 추출 (Cookie)
        String refreshToken = extractCookie(request, "refresh_token");

        // 새 Access Token 발급 + 사용자 정보 조회
        AuthService.AuthResult result = authService.refreshAccessToken(refreshToken);

        // Access Token → httpOnly Cookie (Refresh Token은 그대로 유지)
        setCookie(response, "access_token", result.tokens().getAccessToken(), 30 * 60, "/");

        // 사용자 정보 → 응답 body
        AuthResponse authResponse = AuthResponse.from(result.user());
        return ResponseEntity.ok(ApiResponse.success("token_refreshed", authResponse));
    }

    /**
     * Cookie 설정 헬퍼 메서드
     * @param name 쿠키 이름
     * @param value 쿠키 값
     * @param maxAge 만료 시간 (초)
     * @param path 쿠키 경로
     */
    private void setCookie(HttpServletResponse response, String name, String value, int maxAge, String path) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);  // TODO: 운영 환경에서는 true (HTTPS)
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    /**
     * Cookie 추출 헬퍼 메서드
     * @param name 쿠키 이름
     * @return 쿠키 값 (없으면 null)
     */
    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
