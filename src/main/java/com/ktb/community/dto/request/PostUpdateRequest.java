package com.ktb.community.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 수정 요청 DTO
 * API.md Section 3.4 참조
 * PATCH는 부분 업데이트이므로 모든 필드 선택
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostUpdateRequest {

    @Size(max = 27, message = "제목은 최대 27자입니다")
    private String title;

    private String content;

    /**
     * 선택: 게시글 이미지 ID
     * POST /images로 먼저 업로드 필요 (Phase 3.5+)
     */
    private Long imageId;

    /**
     * PATCH 부분 업데이트 검증: 최소 1개 필드 필요
     * Bean Validation에서 자동 검증
     */
    @AssertTrue(message = "최소 1개 필드가 필요합니다")
    public boolean isHasAnyUpdate() {
        return title != null || content != null || imageId != null;
    }
}
