package com.ktb.community.dto.request;

import com.ktb.community.entity.Post;
import com.ktb.community.entity.User;
import com.ktb.community.enums.PostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 작성 요청 DTO
 * API.md Section 3.3 참조
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCreateRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 27, message = "제목은 최대 27자입니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    private String content;

    /**
     * 선택: 게시글 이미지 ID
     * POST /images로 먼저 업로드 필요 (Phase 3.5+)
     */
    private Long imageId;

    /**
     * DTO → Entity 변환
     *
     * @param user 작성자 (인증된 사용자)
     * @return Post 엔티티
     */
    public Post toEntity(User user) {
        return Post.builder()
                .title(title)
                .content(content)
                .status(PostStatus.ACTIVE)
                .user(user)
                .build();
    }
}
