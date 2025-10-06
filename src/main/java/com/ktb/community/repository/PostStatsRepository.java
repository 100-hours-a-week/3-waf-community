package com.ktb.community.repository;

import com.ktb.community.entity.PostStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * PostStats 엔티티 Repository
 * LLD.md Section 7.2, 12.3 참조 - 동시성 제어
 */
@Repository
public interface PostStatsRepository extends JpaRepository<PostStats, Long> {
    
    /**
     * 조회수 원자적 증가
     * Phase 3에서 PostService.getPostDetail()에서 호출
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PostStats ps SET ps.viewCount = ps.viewCount + 1, " +
           "ps.lastUpdated = CURRENT_TIMESTAMP WHERE ps.postId = :postId")
    int incrementViewCount(@Param("postId") Long postId);
    
    /**
     * 좋아요 수 원자적 증가
     * Phase 3에서 LikeService.likePost()에서 호출
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PostStats ps SET ps.likeCount = ps.likeCount + 1, " +
           "ps.lastUpdated = CURRENT_TIMESTAMP WHERE ps.postId = :postId")
    int incrementLikeCount(@Param("postId") Long postId);
    
    /**
     * 좋아요 수 원자적 감소
     * Phase 3에서 LikeService.unlikePost()에서 호출
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PostStats ps SET ps.likeCount = ps.likeCount - 1, " +
           "ps.lastUpdated = CURRENT_TIMESTAMP " +
           "WHERE ps.postId = :postId AND ps.likeCount > 0")
    int decrementLikeCount(@Param("postId") Long postId);
    
    /**
     * 댓글 수 원자적 증가
     * Phase 3에서 CommentService.createComment()에서 호출
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PostStats ps SET ps.commentCount = ps.commentCount + 1, " +
           "ps.lastUpdated = CURRENT_TIMESTAMP WHERE ps.postId = :postId")
    int incrementCommentCount(@Param("postId") Long postId);
    
    /**
     * 댓글 수 원자적 감소
     * Phase 3에서 CommentService.deleteComment()에서 호출
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PostStats ps SET ps.commentCount = ps.commentCount - 1, " +
           "ps.lastUpdated = CURRENT_TIMESTAMP " +
           "WHERE ps.postId = :postId AND ps.commentCount > 0")
    int decrementCommentCount(@Param("postId") Long postId);
}
