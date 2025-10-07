package com.ktb.community.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtTokenProvider 단위 테스트
 */
@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private String testSecret;
    private long accessTokenValidity;
    private long refreshTokenValidity;

    @BeforeEach
    void setUp() {
        // 테스트용 JWT 설정 (최소 32바이트)
        testSecret = "testSecretKeytestSecretKeytestSecretKey"; // 40 bytes
        accessTokenValidity = 1800000L;  // 30분
        refreshTokenValidity = 604800000L; // 7일

        jwtTokenProvider = new JwtTokenProvider(testSecret, accessTokenValidity, refreshTokenValidity);
    }

    @Test
    @DisplayName("Access Token 생성 - 성공")
    void createAccessToken_Success() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        String role = "USER";

        // When
        String token = jwtTokenProvider.createAccessToken(userId, email, role);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT 형식: header.payload.signature
    }

    @Test
    @DisplayName("Refresh Token 생성 - 성공")
    void createRefreshToken_Success() {
        // Given
        Long userId = 1L;

        // When
        String token = jwtTokenProvider.createRefreshToken(userId);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("유효한 토큰 검증 - true 반환")
    void validateToken_ValidToken_ReturnsTrue() {
        // Given: 유효한 토큰 생성
        String token = jwtTokenProvider.createAccessToken(1L, "test@example.com", "USER");

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰 검증 - false 반환")
    void validateToken_ExpiredToken_ReturnsFalse() {
        // Given: 이미 만료된 토큰 생성 (validity = -1000ms)
        JwtTokenProvider expiredProvider = new JwtTokenProvider(testSecret, -1000L, refreshTokenValidity);
        String expiredToken = expiredProvider.createAccessToken(1L, "test@example.com", "USER");

        // 토큰이 만료될 때까지 대기
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        boolean isValid = jwtTokenProvider.validateToken(expiredToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("잘못된 서명 토큰 검증 - false 반환")
    void validateToken_InvalidSignature_ReturnsFalse() {
        // Given: 다른 secret으로 생성한 토큰
        String differentSecret = "differentSecretKeydifferentSecretKey"; // 40 bytes
        JwtTokenProvider differentProvider = new JwtTokenProvider(differentSecret, accessTokenValidity, refreshTokenValidity);
        String tokenWithDifferentSignature = differentProvider.createAccessToken(1L, "test@example.com", "USER");

        // When: 원래 provider로 검증
        boolean isValid = jwtTokenProvider.validateToken(tokenWithDifferentSignature);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("토큰에서 userId 추출 - 성공")
    void getUserIdFromToken_ReturnsUserId() {
        // Given
        Long expectedUserId = 123L;
        String token = jwtTokenProvider.createAccessToken(expectedUserId, "test@example.com", "USER");

        // When
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        // Then
        assertThat(userId).isEqualTo(expectedUserId);
    }

    @Test
    @DisplayName("토큰에서 email 추출 - 성공")
    void getEmailFromToken_ReturnsEmail() {
        // Given
        String expectedEmail = "test@example.com";
        String token = jwtTokenProvider.createAccessToken(1L, expectedEmail, "USER");

        // When
        String email = jwtTokenProvider.getEmailFromToken(token);

        // Then
        assertThat(email).isEqualTo(expectedEmail);
    }

    @Test
    @DisplayName("토큰 만료 확인 (만료되지 않음) - false 반환")
    void isTokenExpired_NotExpired_ReturnsFalse() {
        // Given: 유효한 토큰
        String token = jwtTokenProvider.createAccessToken(1L, "test@example.com", "USER");

        // When
        boolean isExpired = jwtTokenProvider.isTokenExpired(token);

        // Then
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("토큰 만료 확인 (만료됨) - true 반환")
    void isTokenExpired_Expired_ReturnsTrue() {
        // Given: 이미 만료된 토큰 생성
        JwtTokenProvider expiredProvider = new JwtTokenProvider(testSecret, -1000L, refreshTokenValidity);
        String expiredToken = expiredProvider.createAccessToken(1L, "test@example.com", "USER");

        // 토큰이 만료될 때까지 대기
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        boolean isExpired = jwtTokenProvider.isTokenExpired(expiredToken);

        // Then
        assertThat(isExpired).isTrue();
    }
}
