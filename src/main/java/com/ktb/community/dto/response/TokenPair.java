package com.ktb.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 토큰 쌍 (내부 전달용)
 * - Service → Controller 토큰 전달에만 사용
 * - API 응답 body에는 포함되지 않음 (HttpOnly Cookie로만 전달)
 */
@Getter
@AllArgsConstructor
public class TokenPair {
    private final String accessToken;
    private final String refreshToken;
}
