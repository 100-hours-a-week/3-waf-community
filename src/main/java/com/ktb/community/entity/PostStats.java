package com.ktb.community.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 게시글 통계 엔티티
 * DDL: post_stats 테이블 (1:1 관계)
 */
@Entity
@Table(name = "post_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"post"})
public class PostStats {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    private Integer commentCount = 0;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "last_updated", nullable = false, insertable = false, updatable = false)
    private LocalDateTime lastUpdated;

    @Builder
    public PostStats(Post post) {
        this.post = post;
        this.likeCount = 0;
        this.commentCount = 0;
        this.viewCount = 0;
    }

    // 통계 업데이트 메서드
    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void incrementCommentCount() {
        this.commentCount++;
    }

    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    public void incrementViewCount() {
        this.viewCount++;
    }
}
