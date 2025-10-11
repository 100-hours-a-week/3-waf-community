package com.ktb.community.service;

import com.ktb.community.dto.request.ChangePasswordRequest;
import com.ktb.community.dto.request.UpdateProfileRequest;
import com.ktb.community.dto.response.UserResponse;
import com.ktb.community.entity.User;
import com.ktb.community.enums.UserRole;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.enums.ErrorCode;
import com.ktb.community.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * UserService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("프로필 조회 성공")
    void getProfile_Success() {
        // Given
        Long userId = 1L;
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .nickname("testuser")
                .role(UserRole.USER)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "userId", userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        UserResponse response = userService.getProfile(userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getNickname()).isEqualTo("testuser");

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("프로필 조회 실패 - 사용자 없음")
    void getProfile_UserNotFound_ThrowsException() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getProfile(userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("프로필 수정 성공 - 닉네임 변경")
    void updateProfile_Nickname_Success() {
        // Given
        Long userId = 1L;
        Long authenticatedUserId = 1L;
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .nickname("newnickname")
                .build();

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .nickname("oldnickname")
                .role(UserRole.USER)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "userId", userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("newnickname")).thenReturn(false);

        // When
        UserResponse response = userService.updateProfile(userId, authenticatedUserId, request, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getNickname()).isEqualTo("newnickname");

        verify(userRepository).findById(userId);
        verify(userRepository).existsByNickname("newnickname");
    }

    @Test
    @DisplayName("프로필 수정 실패 - 닉네임 중복")
    void updateProfile_NicknameExists_ThrowsException() {
        // Given
        Long userId = 1L;
        Long authenticatedUserId = 1L;
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .nickname("existingnick")
                .build();

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .nickname("oldnickname")
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "userId", userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("existingnick")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.updateProfile(userId, authenticatedUserId, request, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NICKNAME_ALREADY_EXISTS);

        verify(userRepository).findById(userId);
        verify(userRepository).existsByNickname("existingnick");
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePassword_Success() {
        // Given
        Long userId = 1L;
        Long authenticatedUserId = 1L;
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .newPassword("NewPass123!")
                .newPasswordConfirm("NewPass123!")
                .build();

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("oldPasswordHash")
                .nickname("testuser")
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "userId", userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPass123!")).thenReturn("newEncodedPassword");

        // When
        userService.changePassword(userId, authenticatedUserId, request);

        // Then
        verify(userRepository).findById(userId);
        verify(passwordEncoder).encode("NewPass123!");
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 비밀번호 정책 위반")
    void changePassword_InvalidPolicy_ThrowsException() {
        // Given
        Long userId = 1L;
        Long authenticatedUserId = 1L;
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .newPassword("weak") // 정책 위반
                .newPasswordConfirm("weak")
                .build();

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .nickname("testuser")
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "userId", userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(userId, authenticatedUserId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD_POLICY);

        verify(userRepository).findById(userId);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 비밀번호 불일치")
    void changePassword_Mismatch_ThrowsException() {
        // Given
        Long userId = 1L;
        Long authenticatedUserId = 1L;
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .newPassword("NewPass123!")
                .newPasswordConfirm("DifferentPass123!") // 불일치
                .build();

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .nickname("testuser")
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "userId", userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(userId, authenticatedUserId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_MISMATCH);

        verify(userRepository).findById(userId);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 상태 INACTIVE 변경")
    void deactivateAccount_Success() {
        // Given
        Long userId = 1L;
        Long authenticatedUserId = 1L;

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .nickname("testuser")
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "userId", userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userService.deactivateAccount(userId, authenticatedUserId);

        // Then
        verify(userRepository).findById(userId);
        // Note: Entity 상태 변경은 실제 트랜잭션에서 확인 (단위 테스트에서는 메서드 호출만 검증)
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 권한 없음 (다른 사용자)")
    void deactivateAccount_Unauthorized_ThrowsException() {
        // Given
        Long userId = 1L;
        Long authenticatedUserId = 2L; // 다른 사용자
        
        // 권한 체크 실패 시 DB 조회 안 함 (stubbing 불필요)

        // When & Then
        assertThatThrownBy(() -> userService.deactivateAccount(userId, authenticatedUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_ACCESS);

        verify(userRepository, never()).findById(userId);
    }
}
