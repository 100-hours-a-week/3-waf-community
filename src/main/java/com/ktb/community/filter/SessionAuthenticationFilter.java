package com.ktb.community.filter;

import com.ktb.community.dto.ApiResponse;
import com.ktb.community.dto.response.ErrorDetails;
import com.ktb.community.session.Session;
import com.ktb.community.session.SessionManager;
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
import java.util.Optional;
import java.util.Set;

/**
 * 세션 인증 필터
 * - 순서: 1 (Spring Security보다 먼저 실행)
 * - 공개 엔드포인트는 통과
 * - 세션 검증 후 Request Attribute에 userId 저장
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SessionAuthenticationFilter implements Filter {

    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;

    // SecurityConfig의 공개 엔드포인트 참조
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/login",
            "/auth/logout",
            "/users/signup",
            "/terms",
            "/privacy"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String uri = req.getRequestURI();
        String method = req.getMethod();

        log.debug("[SessionFilter] {} {}", method, uri);

        // OPTIONS 요청은 통과 (CORS Preflight)
        if (HttpMethod.OPTIONS.matches(method)) {
            chain.doFilter(request, response);
            return;
        }

        // 공개 엔드포인트는 통과
        if (isPublicEndpoint(uri, method)) {
            log.debug("[SessionFilter] 공개 엔드포인트 통과: {}", uri);
            chain.doFilter(request, response);
            return;
        }

        // 세션 ID 추출
        String sessionId = extractSessionId(req);
        if (sessionId == null) {
            sendUnauthorized(res, "No session cookie");
            return;
        }

        // 세션 검증
        Optional<Session> sessionOpt = sessionManager.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            sendUnauthorized(res, "Invalid or expired session");
            return;
        }

        Session session = sessionOpt.get();
        if (session.isExpired()) {
            sendUnauthorized(res, "Session expired");
            return;
        }

        // Request Attribute에 userId 저장
        req.setAttribute("userId", session.getUserId());
        log.debug("[SessionFilter] 인증 성공: userId={}", session.getUserId());

        chain.doFilter(request, response);
    }

    /**
     * 공개 엔드포인트 판단 (SecurityConfig 규칙 참조)
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
     * 세션 ID 추출 (Cookie)
     */
    private String extractSessionId(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("SESSIONID".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
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
        log.warn("[SessionFilter] 인증 실패: {}", message);
    }
}
