package com.ktb.community.service;

import com.ktb.community.dto.request.ChangePasswordRequest;
import com.ktb.community.dto.request.UpdateProfileRequest;
import com.ktb.community.dto.response.UserResponse;
import com.ktb.community.entity.User;
import com.ktb.community.enums.UserStatus;
import com.ktb.community.exception.DuplicateResourceException;
import com.ktb.community.exception.ForbiddenException;
import com.ktb.community.exception.InvalidRequestException;
import com.ktb.community.exception.ResourceNotFoundException;
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
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
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
            throw new ForbiddenException("You can only update your own profile");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        // 닉네임 변경 시 중복 확인
        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(request.getNickname())) {
                throw new DuplicateResourceException("nickname", request.getNickname());
            }
            user.updateNickname(request.getNickname());
        }
        
        // 프로필 이미지 변경 (Phase 4에서 구현)
        if (request.getProfileImage() != null) {
            // TODO: Phase 4에서 Image 엔티티 연동
            log.info("Profile image update requested (Phase 4)");
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
            throw new ForbiddenException("You can only change your own password");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        // 비밀번호 확인 일치 검증
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new InvalidRequestException("Password confirmation does not match");
        }
        
        // 비밀번호 정책 검증
        if (!PasswordValidator.isValid(request.getNewPassword())) {
            throw new InvalidRequestException(PasswordValidator.getPolicyDescription());
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
            throw new ForbiddenException("You can only deactivate your own account");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        // 상태 변경 (Soft Delete)
        user.updateStatus(UserStatus.INACTIVE);
        
        log.info("User account deactivated: {}", user.getEmail());
    }
    
    /**
     * 이메일로 사용자 ID 조회 (Controller 인증용)
     */
    @Transactional(readOnly = true)
    public Long findUserIdByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return user.getUserId();
    }
}
