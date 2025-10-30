package com.ktb.community.filter;

import com.ktb.community.dto.ApiResponse;
import com.ktb.community.dto.ErrorDetails;
import com.ktb.community.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * JWT 인증 필터 (순수 Servlet Filter)
 * - 순서: 1 (가장 먼저 실행)
 * - 공개 엔드포인트는 통과
 * - JWT 검증 후 Request Attribute에 userId 저장
 * - SessionAuthenticationFilter 패턴 재사용
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements Filter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    // 공개 엔드포인트 (인증 불필요)
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/login",
            "/auth/logout",     // RT 쿠키만 사용 (AT 불필요)
            "/auth/refresh_token",
            "/users/signup",
            "/users",          // 회원가입 alias
            "/terms",
            "/privacy",
            "/stats"           // 통계 API
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String uri = req.getRequestURI();
        String method = req.getMethod();

        log.debug("[JwtFilter] {} {}", method, uri);

        // OPTIONS 요청은 통과 (CORS Preflight)
        if (HttpMethod.OPTIONS.matches(method)) {
            chain.doFilter(request, response);
            return;
        }

        // 공개 엔드포인트는 선택적 인증 (JWT 있으면 검증, 없으면 통과)
        if (isPublicEndpoint(uri, method)) {
            log.debug("[JwtFilter] 공개 엔드포인트 - 선택적 인증: {}", uri);

            // JWT 추출 시도
            String jwt = extractJwt(req);
            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                if (userId != null) {
                    req.setAttribute("userId", userId);
                    log.debug("[JwtFilter] 로그인 상태로 공개 엔드포인트 접근: userId={}", userId);
                }
            }
            // JWT 없거나 유효하지 않아도 통과

            chain.doFilter(request, response);
            return;
        }

        // JWT 추출 (Cookie → Authorization header)
        String jwt = extractJwt(req);
        if (jwt == null) {
            sendUnauthorized(res, "No access token");
            return;
        }

        // JWT 검증
        if (!jwtTokenProvider.validateToken(jwt)) {
            sendUnauthorized(res, "Invalid or expired token");
            return;
        }

        // JWT payload에서 userId 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
        if (userId == null) {
            sendUnauthorized(res, "Invalid token payload");
            return;
        }

        // Request Attribute에 userId 저장
        req.setAttribute("userId", userId);
        log.debug("[JwtFilter] 인증 성공");

        chain.doFilter(request, response);
    }

    /**
     * 공개 엔드포인트 판단
     */
    private boolean isPublicEndpoint(String uri, String method) {
        // 1. 완전 공개 경로
        if (PUBLIC_PATHS.contains(uri)) {
            return true;
        }

        // 2. GET 요청 공개 (단, /posts/users/me/likes는 인증 필요)
        if (HttpMethod.GET.matches(method)) {
            if (uri.equals("/posts/users/me/likes")) {
                return false;  // 인증 필요
            }

            // /posts, /posts/{id}, /posts/{id}/comments, /users/{id} 공개
            if (uri.startsWith("/posts") || uri.startsWith("/users/")) {
                return true;
            }
        }

        return false;
    }

    /**
     * JWT 추출 (Authorization header)
     * - AT는 클라이언트 JS 변수로 관리 (응답 body의 accessToken 필드)
     * - 클라이언트가 Authorization: Bearer {token} 헤더로 전송
     */
    private String extractJwt(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    /**
     * 401 Unauthorized 응답
     */
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        ErrorDetails errorDetails = ErrorDetails.of(message);
        ApiResponse<ErrorDetails> apiResponse = ApiResponse.error("AUTH-002", errorDetails);
        apiResponse = new ApiResponse<>(
                apiResponse.getMessage(),
                apiResponse.getData(),
                LocalDateTime.now()
        );

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        log.warn("[JwtFilter] 인증 실패: {}", message);
    }
}
