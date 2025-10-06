package com.ktb.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 변경 요청 DTO
 * API.md Section 2.4 참조
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {
    
    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8-20자여야 합니다")
    private String newPassword;
    
    @NotBlank(message = "비밀번호 확인은 필수입니다")
    private String newPasswordConfirm;
}
