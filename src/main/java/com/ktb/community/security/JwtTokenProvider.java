package com.ktb.community.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * [세션 전환]
 * 현재는 세션 기반 인증(SessionAuthenticationFilter)을 사용하므로 이 클래스는 실행되지 않습니다.
 * JWT 인프라는 향후 토큰 기반 인증으로 전환 시 재사용을 위해 보존되었습니다.
 * 
 * 관련 파일:
 * - SessionAuthenticationFilter: 세션 쿠키 검증 및 인증 처리
 * - SessionManager: 세션 생성/조회/삭제
 * - InMemorySessionStore: 세션 저장소 (ConcurrentHashMap)
 */

/**
 * JWT 토큰 생성 및 검증을 담당하는 Provider
 * LLD.md Section 6.1-6.2 참조
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidity) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    /**
     * Access Token 생성 (30분)
     * @param userId 사용자 ID
     * @param email 사용자 이메일
     * @param role 사용자 권한
     * @return JWT Access Token
     */
    public String createAccessToken(Long userId, String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidity);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Refresh Token 생성 (7일)
     * @param userId 사용자 ID
     * @return JWT Refresh Token
     */
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenValidity);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    /**
     * JWT 토큰에서 이메일 추출
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("email", String.class);
    }

    /**
     * JWT 토큰 유효성 검증
     * @param token JWT 토큰
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * JWT 토큰 만료 여부 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }
}
