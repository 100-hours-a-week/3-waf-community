package com.ktb.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 갱신 요청 DTO
 * API.md Section 1.3 참조
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {
    
    @NotBlank(message = "리프레시 토큰은 필수입니다")
    private String refreshToken;
}
