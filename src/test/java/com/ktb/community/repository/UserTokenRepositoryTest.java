package com.ktb.community.repository;

import com.ktb.community.entity.User;
import com.ktb.community.entity.UserToken;
import com.ktb.community.enums.UserRole;
import com.ktb.community.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserTokenRepository 단위 테스트
 */
@DataJpaTest
@DisplayName("UserTokenRepository 테스트")
class UserTokenRepositoryTest {

    @Autowired
    private UserTokenRepository userTokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private UserToken testToken;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .nickname("testuser")
                .role(UserRole.USER)
                .userStatus(UserStatus.ACTIVE)
                .build();
        entityManager.persist(testUser);

        // 테스트 토큰 생성
        testToken = UserToken.builder()
                .token("test-refresh-token")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .user(testUser)
                .userAgent("Mozilla/5.0")
                .ipAddress("127.0.0.1")
                .build();
        entityManager.persist(testToken);
        entityManager.flush();
    }

    @Test
    @DisplayName("Refresh Token 저장 - 성공")
    void save_Success() {
        // Given
        UserToken newToken = UserToken.builder()
                .token("new-refresh-token")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .user(testUser)
                .userAgent("Chrome")
                .ipAddress("192.168.0.1")
                .build();

        // When
        UserToken saved = userTokenRepository.save(newToken);
        entityManager.flush();

        // Then
        assertThat(saved.getUserTokenId()).isNotNull();
        assertThat(saved.getToken()).isEqualTo("new-refresh-token");
        assertThat(saved.getUser().getUserId()).isEqualTo(testUser.getUserId());
    }

    @Test
    @DisplayName("토큰으로 UserToken 조회 - 성공")
    void findByToken_ReturnsUserToken() {
        // When
        Optional<UserToken> result = userTokenRepository.findByToken("test-refresh-token");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo("test-refresh-token");
        assertThat(result.get().getUser().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("사용자 ID와 토큰으로 UserToken 조회 - 성공")
    void findByUserUserIdAndToken_ReturnsToken() {
        // When
        Optional<UserToken> result = userTokenRepository.findByUserUserIdAndToken(
                testUser.getUserId(), "test-refresh-token");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo("test-refresh-token");
    }

    @Test
    @DisplayName("특정 토큰 삭제 (로그아웃) - 성공")
    void deleteByToken_Success() {
        // When
        userTokenRepository.deleteByToken("test-refresh-token");
        entityManager.flush();

        // Then
        Optional<UserToken> result = userTokenRepository.findByToken("test-refresh-token");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("특정 사용자의 모든 토큰 삭제 - 성공")
    void deleteByUserUserId_DeletesAllUserTokens() {
        // Given: 같은 사용자의 추가 토큰 생성
        UserToken secondToken = UserToken.builder()
                .token("second-refresh-token")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .user(testUser)
                .build();
        entityManager.persist(secondToken);
        entityManager.flush();

        // When: 해당 사용자의 모든 토큰 삭제
        userTokenRepository.deleteByUserUserId(testUser.getUserId());
        entityManager.flush();

        // Then: 모든 토큰 삭제 확인
        Optional<UserToken> firstResult = userTokenRepository.findByToken("test-refresh-token");
        Optional<UserToken> secondResult = userTokenRepository.findByToken("second-refresh-token");

        assertThat(firstResult).isEmpty();
        assertThat(secondResult).isEmpty();
    }

    @Test
    @DisplayName("만료된 토큰 삭제 (배치 작업) - 성공")
    void deleteExpiredTokens_Success() {
        // Given: 만료된 토큰 추가
        UserToken expiredToken = UserToken.builder()
                .token("expired-token")
                .expiresAt(LocalDateTime.now().minusDays(1)) // 이미 만료
                .user(testUser)
                .build();
        entityManager.persist(expiredToken);
        entityManager.flush();

        // When: 만료된 토큰 삭제
        int deletedCount = userTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        entityManager.flush();

        // Then: 만료된 토큰만 삭제, 유효한 토큰은 유지
        assertThat(deletedCount).isEqualTo(1);

        Optional<UserToken> expiredResult = userTokenRepository.findByToken("expired-token");
        Optional<UserToken> validResult = userTokenRepository.findByToken("test-refresh-token");

        assertThat(expiredResult).isEmpty();
        assertThat(validResult).isPresent();
    }
}
