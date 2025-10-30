package com.ktb.community.repository;

import com.ktb.community.entity.Comment;
import com.ktb.community.enums.CommentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 댓글 Repository
 * FR-COMMENT-001~004
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 댓글 목록 조회 (Fetch Join)
     */
    @Query("SELECT c FROM Comment c " +
           "JOIN FETCH c.user " +
           "WHERE c.post.postId = :postId AND c.commentStatus = :status " +
           "ORDER BY c.createdAt DESC")
    Page<Comment> findByPostIdAndStatusWithUser(
            @Param("postId") Long postId,
            @Param("status") CommentStatus status,
            Pageable pageable
    );

    /**
     * 댓글 상세 조회 (Fetch Join)
     */
    @Query("SELECT c FROM Comment c " +
           "JOIN FETCH c.user " +
           "WHERE c.commentId = :commentId AND c.commentStatus = :status")
    Optional<Comment> findByIdAndStatusWithUser(
            @Param("commentId") Long commentId,
            @Param("status") CommentStatus status
    );

    /**
     * 댓글 수 카운트 (통계 검증용)
     */
    long countByPostPostIdAndCommentStatus(Long postId, CommentStatus status);

    /**
     * 상태별 전체 댓글 수 조회
     */
    long countByCommentStatus(CommentStatus status);
}
