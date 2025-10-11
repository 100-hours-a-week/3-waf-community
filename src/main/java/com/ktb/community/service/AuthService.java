package com.ktb.community.service;

import com.ktb.community.dto.request.LoginRequest;
import com.ktb.community.dto.request.SignupRequest;
import com.ktb.community.dto.response.AuthResponse;
import com.ktb.community.entity.User;
import com.ktb.community.entity.UserToken;
import com.ktb.community.enums.UserStatus;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.enums.ErrorCode;
import com.ktb.community.repository.UserRepository;
import com.ktb.community.repository.UserTokenRepository;
import com.ktb.community.security.JwtTokenProvider;
import com.ktb.community.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * 인증 서비스
 * LLD.md Section 7.1, PRD.md FR-AUTH-001~004 참조
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final ImageService imageService;
    private final com.ktb.community.repository.ImageRepository imageRepository;
    
    /**
     * 회원가입 (FR-AUTH-001)
     * - 이메일/닉네임 중복 확인
     * - 비밀번호 정책 검증
     * - 프로필 이미지 업로드 (Multipart)
     * - 자동 로그인 (토큰 발급)
     */
    @Transactional
    public AuthResponse signup(SignupRequest request, MultipartFile profileImage) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, 
                    "Email already exists: " + request.getEmail());
        }
        
        // 닉네임 중복 확인
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS, 
                    "Nickname already exists: " + request.getNickname());
        }
        
        // 비밀번호 정책 검증
        if (!PasswordValidator.isValid(request.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_POLICY, 
                    PasswordValidator.getPolicyDescription());
        }
        
        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        
        // 프로필 이미지 업로드 (있을 경우)
        com.ktb.community.entity.Image image = null;
        if (profileImage != null && !profileImage.isEmpty()) {
            com.ktb.community.dto.response.ImageResponse imageResponse = imageService.uploadImage(profileImage);
            image = imageRepository.findById(imageResponse.getImageId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
            image.clearExpiresAt();  // 영구 보존
            log.info("Profile image uploaded for signup: imageId={}", image.getImageId());
        }
        
        // 사용자 생성
        User user = request.toEntity(encodedPassword);
        if (image != null) {
            user.updateProfileImage(image);
        }
        User savedUser = userRepository.save(user);

        log.info("User registered: {}", savedUser.getEmail());
        
        // 자동 로그인 - 토큰 발급
        return generateTokens(savedUser);
    }
    
    /**
     * 로그인 (FR-AUTH-002)
     * - 이메일/비밀번호 검증
     * - 계정 상태 확인 (ACTIVE만 허용)
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        
        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        
        // 계정 상태 확인
        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }
        
        log.info("User logged in: {}", user.getEmail());
        
        return generateTokens(user);
    }
    
    /**
     * 로그아웃 (FR-AUTH-003)
     * - Refresh Token DB에서 삭제
     */
    @Transactional
    public void logout(String refreshToken) {
        userTokenRepository.deleteByToken(refreshToken);
        log.info("User logged out");
    }
    
    /**
     * Access Token 재발급 (FR-AUTH-004)
     * - Refresh Token 유효성 검증
     * - 새 Access Token 발급
     */
    @Transactional(readOnly = true)
    public AuthResponse refreshAccessToken(String refreshToken) {
        // Refresh Token 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        
        // DB에서 토큰 확인
        UserToken userToken = userTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        
        // 만료 확인
        if (userToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }
        
        // 사용자 조회
        User user = userToken.getUser();
        
        // 새 Access Token 발급
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name()
        );
        
        log.info("Access token refreshed for user: {}", user.getEmail());
        
        return AuthResponse.accessOnly(accessToken);
    }
    
    /**
     * 토큰 생성 및 저장 (공통 메서드)
     */
    private AuthResponse generateTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name()
        );
        
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());
        
        // Refresh Token DB 저장
        UserToken userToken = UserToken.builder()
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .user(user)
                .build();
        
        userTokenRepository.save(userToken);
        
        return AuthResponse.of(accessToken, refreshToken);
    }
}
