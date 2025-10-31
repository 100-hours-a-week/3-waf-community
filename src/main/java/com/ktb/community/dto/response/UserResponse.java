package com.ktb.community.dto.response;

import com.ktb.community.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 정보 응답 DTO
 * API.md Section 2.2, 2.3 참조
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private Long userId;
    private String nickname;
    private String email;
    private String profileImage;
    
    /**
     * Entity → DTO 변환
     */
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImage(user.getProfileImage() != null ? user.getProfileImage().getImageUrl() : null)
                .build();
    }
}
