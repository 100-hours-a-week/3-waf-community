package com.ktb.community.controller;

import com.ktb.community.config.RateLimit;
import com.ktb.community.dto.ApiResponse;
import com.ktb.community.dto.request.LoginRequest;
// [세션 전환] JWT 방식 (미사용)
// import com.ktb.community.dto.request.RefreshTokenRequest;
// import com.ktb.community.dto.request.SignupRequest;  // 회원가입은 UserController에서 처리
import com.ktb.community.dto.response.AuthResponse;
import com.ktb.community.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
// import org.springframework.http.HttpStatus;  // 미사용
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
     * httpOnly Cookie 방식으로 세션 ID 전달 (XSS 방어)
     */
    @PostMapping("/login")
    @RateLimit(requestsPerMinute = 5)
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthService.AuthResult result = authService.login(request);

        // 세션 ID → httpOnly Cookie 설정 (1시간)
        setCookie(response, "SESSIONID", result.sessionId(), 3600, "/");

        // 사용자 정보 → 응답 body
        AuthResponse authResponse = AuthResponse.from(result.user());
        return ResponseEntity.ok(ApiResponse.success("login_success", authResponse));
    }
    
    /**
     * 로그아웃 (API.md Section 1.2)
     * POST /auth/logout
     * Cookie에서 세션 ID 추출 및 삭제
     * Tier 3: 제한 없음 (공격 동인 없음)
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        // 세션 ID 추출 (Cookie)
        String sessionId = extractCookie(request, "SESSIONID");

        // 세션 삭제
        if (sessionId != null) {
            authService.logout(sessionId);
        }

        // 쿠키 삭제 (MaxAge=0)
        Cookie sessionCookie = new Cookie("SESSIONID", null);
        sessionCookie.setMaxAge(0);
        sessionCookie.setPath("/");
        response.addCookie(sessionCookie);

        return ResponseEntity.ok(ApiResponse.success("logout_success"));
    }
    
    // [세션 전환] JWT refresh 엔드포인트 (미사용)
    // /**
    //  * 액세스 토큰 재발급 (API.md Section 1.3)
    //  * POST /auth/refresh_token
    //  * Cookie에서 Refresh Token 추출하여 새 Access Token 발급
    //  * 사용자 정보도 함께 반환 (프론트엔드 localStorage 동기화용)
    //  * Tier 2: 중간 제한 (비정상 토큰 갱신 감지)
    //  */
    // @PostMapping("/refresh_token")
    // @RateLimit(requestsPerMinute = 30)
    // public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(...) { ... }

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
