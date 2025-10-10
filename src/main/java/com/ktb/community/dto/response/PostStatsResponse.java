package com.ktb.community.dto.response;

import com.ktb.community.entity.PostStats;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 게시글 통계 정보 DTO (Nested)
 * PostResponse에서 사용
 */
@Getter
@Builder
@AllArgsConstructor
public class PostStatsResponse {

    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;

    /**
     * Entity → DTO 변환
     *
     * @param stats PostStats 엔티티
     * @return PostStatsResponse DTO
     */
    public static PostStatsResponse from(PostStats stats) {
        return PostStatsResponse.builder()
                .likeCount(stats.getLikeCount())
                .commentCount(stats.getCommentCount())
                .viewCount(stats.getViewCount())
                .build();
    }
}
