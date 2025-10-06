package com.ktb.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인증 응답 DTO (로그인, 회원가입, 토큰 갱신)
 * API.md Section 1.1, 2.1 참조
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    private String accessToken;
    private String refreshToken;
    
    /**
     * Access Token만 반환 (토큰 갱신 시)
     */
    public static AuthResponse accessOnly(String accessToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .build();
    }
    
    /**
     * Access Token + Refresh Token 반환 (로그인, 회원가입 시)
     */
    public static AuthResponse of(String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
