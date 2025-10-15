package com.ktb.community.repository;

import com.ktb.community.entity.User;
import com.ktb.community.enums.UserRole;
import com.ktb.community.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import com.ktb.community.config.JpaAuditingConfig;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserRepository 단위 테스트
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@DisplayName("UserRepository 테스트")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .nickname("testuser")
                .role(UserRole.USER)
                
                .build();

        entityManager.persist(testUser);
        entityManager.flush();
    }

    @Test
    @DisplayName("이메일로 사용자 조회 - 성공")
    void findByEmail_ReturnsUser() {
        // When
        Optional<User> result = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        assertThat(result.get().getNickname()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("이메일과 상태로 사용자 조회 - ACTIVE만 조회")
    void findByEmailAndUserStatus_ActiveOnly() {
        // Given: INACTIVE 사용자 추가
        User inactiveUser = User.builder()
                .email("inactive@example.com")
                .passwordHash("hashedPassword")
                .nickname("inactive")
                .role(UserRole.USER)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(inactiveUser, "userStatus", UserStatus.INACTIVE);
        entityManager.persist(inactiveUser);
        entityManager.flush();

        // When: ACTIVE 사용자만 조회
        Optional<User> activeResult = userRepository.findByEmailAndUserStatus("test@example.com", UserStatus.ACTIVE);
        Optional<User> inactiveResult = userRepository.findByEmailAndUserStatus("inactive@example.com", UserStatus.ACTIVE);

        // Then
        assertThat(activeResult).isPresent();
        assertThat(activeResult.get().getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(inactiveResult).isEmpty();
    }

    @Test
    @DisplayName("닉네임 존재 여부 확인 - true 반환")
    void existsByNickname_ReturnsTrue() {
        // When
        boolean exists = userRepository.existsByNickname("testuser");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("이메일 존재 여부 확인 - true 반환")
    void existsByEmail_ReturnsTrue() {
        // When
        boolean exists = userRepository.existsByEmail("test@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("userId로 사용자 조회 - 성공")
    void findById_ReturnsUser() {
        // When
        Optional<User> result = userRepository.findById(testUser.getUserId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(testUser.getUserId());
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("존재하지 않는 이메일 조회 - Empty 반환")
    void findByEmail_NotFound_ReturnsEmpty() {
        // When
        Optional<User> result = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("userId와 상태로 사용자 조회 - ACTIVE 성공")
    void findByUserIdAndUserStatus_ActiveUser_Success() {
        // When
        Optional<User> result = userRepository.findByUserIdAndUserStatus(
                testUser.getUserId(), UserStatus.ACTIVE);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(testUser.getUserId());
        assertThat(result.get().getUserStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("userId와 상태로 사용자 조회 - INACTIVE는 조회 안됨")
    void findByUserIdAndUserStatus_InactiveUser_NotFound() {
        // Given: INACTIVE 사용자 생성
        User inactiveUser = User.builder()
                .email("inactive2@example.com")
                .passwordHash("hashedPassword")
                .nickname("inactive2")
                .role(UserRole.USER)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(inactiveUser, "userStatus", UserStatus.INACTIVE);
        entityManager.persist(inactiveUser);
        entityManager.flush();

        // When: ACTIVE로 조회 시도
        Optional<User> result = userRepository.findByUserIdAndUserStatus(
                inactiveUser.getUserId(), UserStatus.ACTIVE);

        // Then: 조회 안됨
        assertThat(result).isEmpty();
    }
}
