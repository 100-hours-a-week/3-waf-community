package com.ktb.community.dto.response;

import com.ktb.community.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인증 응답 DTO (로그인, 회원가입, 토큰 재발급)
 * - AT: 응답 body에 포함 (클라이언트 JS 변수 저장)
 * - RT: HttpOnly Cookie로 전달 (XSS 방어)
 * - 사용자 정보: 응답 body에 포함
 * API.md Section 1.1, 1.3, 2.1 참조
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
    private String accessToken;  // [JWT 전환] AT 추가

    /**
     * User Entity → DTO 변환 (세션 방식 호환)
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

    /**
     * User Entity + AT → DTO 변환 (JWT 방식)
     */
    public static AuthResponse from(User user, String accessToken) {
        return AuthResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage() != null
                    ? user.getProfileImage().getImageUrl()
                    : null)
                .accessToken(accessToken)
                .build();
    }
}
