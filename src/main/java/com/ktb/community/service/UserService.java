package com.ktb.community.service;

import com.ktb.community.dto.request.ChangePasswordRequest;
import com.ktb.community.dto.request.UpdateProfileRequest;
import com.ktb.community.dto.response.UserResponse;
import com.ktb.community.entity.User;
import com.ktb.community.enums.UserStatus;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.enums.ErrorCode;
import com.ktb.community.repository.UserRepository;
import com.ktb.community.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 서비스
 * PRD.md FR-USER-001~004 참조
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 사용자 프로필 조회 (FR-USER-001)
     */
    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, 
                        "User not found with id: " + userId));
        
        return UserResponse.from(user);
    }
    
    /**
     * 사용자 프로필 수정 (FR-USER-002)
     * - 본인만 수정 가능
     * - 닉네임 중복 확인
     */
    @Transactional
    public UserResponse updateProfile(Long userId, Long authenticatedUserId, UpdateProfileRequest request) {
        // 권한 확인
        if (!userId.equals(authenticatedUserId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, 
                        "User not found with id: " + userId));
        
        // 닉네임 변경 시 중복 확인
        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(request.getNickname())) {
                throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS, 
                        "Nickname already exists: " + request.getNickname());
            }
            user.updateNickname(request.getNickname());
        }
        
        // 프로필 이미지 변경 (Phase 3.5+에서 구현)
        if (request.getProfileImageId() != null) {
            // TODO: Phase 3.5+ ImageRepository 추가 후 구현
            // Image image = imageRepository.findById(request.getProfileImageId())
            //     .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
            // user.updateProfileImage(image);
            log.info("Profile image update requested (Phase 3.5+): imageId={}", request.getProfileImageId());
        }
        
        log.info("User profile updated: {}", user.getEmail());
        
        return UserResponse.from(user);
    }
    
    /**
     * 비밀번호 변경 (FR-USER-003)
     * - 본인만 변경 가능
     * - 비밀번호 정책 검증
     * - 비밀번호 확인 일치 검증
     */
    @Transactional
    public void changePassword(Long userId, Long authenticatedUserId, ChangePasswordRequest request) {
        // 권한 확인
        if (!userId.equals(authenticatedUserId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, 
                        "User not found with id: " + userId));
        
        // 비밀번호 확인 일치 검증
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        
        // 비밀번호 정책 검증
        if (!PasswordValidator.isValid(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_POLICY, 
                    PasswordValidator.getPolicyDescription());
        }
        
        // 비밀번호 암호화 및 업데이트
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.updatePassword(encodedPassword);
        
        log.info("Password changed for user: {}", user.getEmail());
    }
    
    /**
     * 회원 탈퇴 (FR-USER-004)
     * - 본인만 탈퇴 가능
     * - Soft Delete (상태 변경)
     */
    @Transactional
    public void deactivateAccount(Long userId, Long authenticatedUserId) {
        // 권한 확인
        if (!userId.equals(authenticatedUserId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, 
                        "User not found with id: " + userId));
        
        // 상태 변경 (Soft Delete)
        user.updateStatus(UserStatus.INACTIVE);
        
        log.info("User account deactivated: {}", user.getEmail());
    }

    /**
     * 이메일로 사용자 ID 조회 (Controller 인증용)
     */
    @Transactional(readOnly = true)
    public Long findUserIdByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .map(User::getUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "User not found with email: " + email));
    }
    
}
