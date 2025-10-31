package com.ktb.community.dto.response;

import com.ktb.community.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인증 응답 DTO (로그인, 회원가입)
 * - 토큰은 HttpOnly Cookie로만 전달
 * - 응답 body에는 사용자 정보만 포함
 * API.md Section 1.1, 2.1 참조
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private Long userId;
    private String email;
    private String nickname;
    private String profileImage;

    /**
     * User Entity → DTO 변환
     */
    public static AuthResponse from(User user) {
        return AuthResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage() != null
                    ? user.getProfileImage().getImageUrl()
                    : null)
                .build();
    }
}
