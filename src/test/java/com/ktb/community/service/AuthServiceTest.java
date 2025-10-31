package com.ktb.community.service;

import com.ktb.community.dto.request.LoginRequest;
import com.ktb.community.dto.request.SignupRequest;
import com.ktb.community.dto.response.AuthResponse;
import com.ktb.community.entity.User;
import com.ktb.community.entity.UserToken;
import com.ktb.community.enums.UserRole;
import com.ktb.community.enums.UserStatus;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.enums.ErrorCode;
import com.ktb.community.repository.UserRepository;
import com.ktb.community.repository.UserTokenRepository;
import com.ktb.community.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AuthService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTokenRepository userTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입 성공 - 토큰 발급")
    void signup_Success() {
        // Given: 요청 데이터 준비
        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .password("Test1234!")
                .nickname("testuser")
                .build(); // profileImage는 null (선택 필드)

        // Given: Mock 동작 정의
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByNickname(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        // Given: 저장 후 반환될 User 객체 생성
        User savedUser = User.builder()
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .nickname("testuser")
                .role(UserRole.USER)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(savedUser, "userId", 1L); // ID 주입
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Given: 토큰 생성 Mock
        when(jwtTokenProvider.createAccessToken(anyLong(), anyString(), anyString()))
                .thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(anyLong()))
                .thenReturn("refresh-token");

        // When: 회원가입 실행
        AuthService.AuthResult result = authService.signup(request);

        // Then: 토큰 검증
        assertThat(result).isNotNull();
        assertThat(result.tokens()).isNotNull();
        assertThat(result.tokens().getAccessToken()).isEqualTo("access-token");
        assertThat(result.tokens().getRefreshToken()).isEqualTo("refresh-token");

        // Then: 사용자 정보 검증
        assertThat(result.user()).isNotNull();
        assertThat(result.user().getUserId()).isEqualTo(1L);
        assertThat(result.user().getEmail()).isEqualTo("test@example.com");
        assertThat(result.user().getNickname()).isEqualTo("testuser");

        // Then: 호출 검증
        verify(userRepository).save(any(User.class)); // User 저장 확인
        verify(userTokenRepository).save(any(UserToken.class));// RefreshToken 저장 확인
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_EmailExists_ThrowsException() {
        // Given
        SignupRequest request = SignupRequest.builder()
                .email("duplicate@example.com")
                .password("Test1234!")
                .nickname("testuser")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 닉네임 중복")
    void signup_NicknameExists_ThrowsException() {
        // Given
        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .password("Test1234!")
                .nickname("duplicatenick")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByNickname(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NICKNAME_ALREADY_EXISTS);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 성공 - 토큰 발급")
    void login_Success() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("Test1234!")
                .build();

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .nickname("testuser")
                .role(UserRole.USER)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "userId", 1L);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(anyLong(), anyString(), anyString()))
                .thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(anyLong()))
                .thenReturn("refresh-token");

        // When
        AuthService.AuthResult result = authService.login(request);

        // Then: 토큰 검증
        assertThat(result).isNotNull();
        assertThat(result.tokens()).isNotNull();
        assertThat(result.tokens().getAccessToken()).isEqualTo("access-token");
        assertThat(result.tokens().getRefreshToken()).isEqualTo("refresh-token");

        // Then: 사용자 정보 검증
        assertThat(result.user()).isNotNull();
        assertThat(result.user().getUserId()).isEqualTo(1L);
        assertThat(result.user().getEmail()).isEqualTo("test@example.com");

        verify(userTokenRepository).save(any(UserToken.class));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_WrongPassword_ThrowsException() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("wrongPassword")
                .build();

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .nickname("testuser")
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "userId", 1L);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);

        verify(jwtTokenProvider, never()).createAccessToken(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("로그인 실패 - 비활성 계정")
    void login_InactiveAccount_ThrowsException() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("Test1234!")
                .build();

        User inactiveUser = User.builder()
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .nickname("testuser")
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(inactiveUser, "userId", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(inactiveUser, "userStatus", UserStatus.INACTIVE); // 비활성 상태

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_INACTIVE);

        verify(jwtTokenProvider, never()).createAccessToken(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("로그아웃 성공 - Refresh Token 삭제 확인")
    void logout_Success_InvokesDelete() {
        // Given
        String refreshToken = "valid-refresh-token";

        // When
        authService.logout(refreshToken);

        // Then
        verify(userTokenRepository, times(1)).deleteByToken(refreshToken);
    }

    @Test
    @DisplayName("Access Token 재발급 성공")
    void refreshAccessToken_Success() {
        // Given
        String refreshToken = "valid-refresh-token";

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .nickname("testuser")
                .role(UserRole.USER)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "userId", 1L);

        UserToken userToken = UserToken.builder()
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .user(user)
                .build();

        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(userTokenRepository.findByToken(refreshToken)).thenReturn(Optional.of(userToken));
        when(jwtTokenProvider.createAccessToken(anyLong(), anyString(), anyString()))
                .thenReturn("new-access-token");

        // When
        AuthService.AuthResult result = authService.refreshAccessToken(refreshToken);

        // Then: 토큰 검증
        assertThat(result).isNotNull();
        assertThat(result.tokens()).isNotNull();
        assertThat(result.tokens().getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.tokens().getRefreshToken()).isEqualTo("valid-refresh-token");

        // Then: 사용자 정보 검증
        assertThat(result.user()).isNotNull();
        assertThat(result.user().getUserId()).isEqualTo(1L);
        assertThat(result.user().getEmail()).isEqualTo("test@example.com");

        verify(jwtTokenProvider).createAccessToken(1L, "test@example.com", "USER");
    }
}
