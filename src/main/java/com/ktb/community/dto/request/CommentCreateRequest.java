package com.ktb.community.dto.request;

import com.ktb.community.entity.Comment;
import com.ktb.community.entity.Post;
import com.ktb.community.entity.User;
import com.ktb.community.enums.CommentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 댓글 작성 요청 DTO
 * API.md Section 5.2 참조
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreateRequest {

    @NotBlank(message = "댓글 내용은 필수입니다")
    @Size(max = 200, message = "댓글은 최대 200자입니다")
    private String comment;

    /**
     * DTO → Entity 변환
     *
     * @param post 게시글
     * @param user 작성자 (인증된 사용자)
     * @return Comment 엔티티
     */
    public Comment toEntity(Post post, User user) {
        return Comment.builder()
                .content(comment)
                .status(CommentStatus.ACTIVE)
                .post(post)
                .user(user)
                .build();
    }
}
