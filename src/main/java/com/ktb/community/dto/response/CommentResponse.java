package com.ktb.community.dto.response;

import com.ktb.community.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 댓글 응답 DTO
 * API.md Section 5 참조
 */
@Getter
@Builder
@AllArgsConstructor
public class CommentResponse {

    private Long commentId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserSummary author;

    /**
     * Entity → DTO 변환
     *
     * @param comment Comment 엔티티 (Fetch Join으로 user 로드 필요)
     * @return CommentResponse DTO
     */
    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .author(UserSummary.from(comment.getUser()))
                .build();
    }
}
