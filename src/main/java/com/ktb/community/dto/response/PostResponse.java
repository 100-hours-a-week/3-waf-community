package com.ktb.community.dto.response;

import com.ktb.community.entity.Post;
import com.ktb.community.entity.PostImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 게시글 응답 DTO
 * API.md Section 3 참조
 */
@Getter
@Builder
@AllArgsConstructor
public class PostResponse {

    private Long postId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserSummary author;
    private PostStatsResponse stats;
    private List<String> images;  // image URLs (display_order 순서)

    /**
     * Entity → DTO 변환
     *
     * @param post Post 엔티티 (Fetch Join으로 user, stats, postImages 로드 필요)
     * @return PostResponse DTO
     */
    public static PostResponse from(Post post) {
        return PostResponse.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .author(UserSummary.from(post.getUser()))
                .stats(post.getStats() != null ? PostStatsResponse.from(post.getStats()) : null)
                .images(post.getPostImages().stream()
                        .sorted(Comparator.comparing(PostImage::getDisplayOrder))
                        .map(pi -> pi.getImage().getImageUrl())
                        .collect(Collectors.toList()))
                .build();
    }
}
