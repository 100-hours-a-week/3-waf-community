package com.ktb.community.repository;

import com.ktb.community.entity.PostStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * PostStats Repository
 * 동시성 제어 (원자적 UPDATE) -> 현재 낙관적 락으로 변경.
 */
@Repository
public interface PostStatsRepository extends JpaRepository<PostStats, Long> {
    
    /**
     * 조회수 원자적 증가
     */
    @Modifying(clearAutomatically = false)
    @Query("UPDATE PostStats ps SET ps.viewCount = ps.viewCount + 1, " +
           "ps.lastUpdated = CURRENT_TIMESTAMP WHERE ps.postId = :postId")
    int incrementViewCount(@Param("postId") Long postId);

    /**
     * 좋아요 수 원자적 증가
     */
    @Modifying(clearAutomatically = false)
    @Query("UPDATE PostStats ps SET ps.likeCount = ps.likeCount + 1, " +
           "ps.lastUpdated = CURRENT_TIMESTAMP WHERE ps.postId = :postId")
    int incrementLikeCount(@Param("postId") Long postId);

    /**
     * 좋아요 수 원자적 감소
     */
    @Modifying(clearAutomatically = false)
    @Query("UPDATE PostStats ps SET ps.likeCount = ps.likeCount - 1, " +
           "ps.lastUpdated = CURRENT_TIMESTAMP " +
           "WHERE ps.postId = :postId AND ps.likeCount > 0")
    int decrementLikeCount(@Param("postId") Long postId);

    /**
     * 댓글 수 원자적 증가
     */
    @Modifying(clearAutomatically = false)
    @Query("UPDATE PostStats ps SET ps.commentCount = ps.commentCount + 1, " +
           "ps.lastUpdated = CURRENT_TIMESTAMP WHERE ps.postId = :postId")
    int incrementCommentCount(@Param("postId") Long postId);

    /**
     * 댓글 수 원자적 감소
     */
    @Modifying(clearAutomatically = false)
    @Query("UPDATE PostStats ps SET ps.commentCount = ps.commentCount - 1, " +
           "ps.lastUpdated = CURRENT_TIMESTAMP " +
           "WHERE ps.postId = :postId AND ps.commentCount > 0")
    int decrementCommentCount(@Param("postId") Long postId);
}
