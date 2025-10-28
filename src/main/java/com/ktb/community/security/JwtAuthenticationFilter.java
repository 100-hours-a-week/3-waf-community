package com.ktb.community.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * [세션 전환]
 * 현재는 세션 기반 인증(SessionAuthenticationFilter)을 사용하므로 이 필터는 실행되지 않습니다.
 * (SecurityConfig에서 필터 체인에 추가하지 않음)
 * JWT 인프라는 향후 토큰 기반 인증으로 전환 시 재사용을 위해 보존되었습니다.
 * 
 * 관련 파일:
 * - SessionAuthenticationFilter: 세션 쿠키 검증 및 인증 처리
 * - SessionManager: 세션 생성/조회/삭제
 * - InMemorySessionStore: 세션 저장소 (ConcurrentHashMap)
 */

/**
 * JWT 인증 필터
 * 모든 요청에 대해 JWT 토큰을 검증하고 SecurityContext에 인증 정보 설정
 * LLD.md Section 6.2 참조
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                
                UserDetails userDetails = userDetailsService.loadUserById(userId);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Set authentication for user: {}", userId);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Cookie 또는 Authorization 헤더에서 JWT 토큰 추출
     * 우선순위: 1) Cookie, 2) Authorization header (하위 호환성)
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        // 1. Cookie에서 토큰 추출 (우선순위)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 2. Authorization header에서 추출 (하위 호환성)
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
