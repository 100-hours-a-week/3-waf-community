package com.ktb.community.repository;

import com.ktb.community.entity.Comment;
import com.ktb.community.enums.CommentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 댓글 Repository
 * PRD.md FR-COMMENT-001~004 참조
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 특정 게시글의 댓글 목록 조회 (ACTIVE만, 생성일 오름차순, N+1 방지)
     * Fetch Join으로 user 정보 함께 조회
     *
     * @param postId 게시글 ID
     * @param status 댓글 상태 (ACTIVE)
     * @param pageable 페이지 정보 (offset, limit)
     * @return 댓글 페이지
     */
    @Query("SELECT c FROM Comment c " +
           "JOIN FETCH c.user " +
           "WHERE c.post.postId = :postId AND c.status = :status " +
           "ORDER BY c.createdAt ASC")
    Page<Comment> findByPostIdAndStatusWithUser(
            @Param("postId") Long postId,
            @Param("status") CommentStatus status,
            Pageable pageable
    );

    /**
     * 댓글 상세 조회 (Fetch Join)
     *
     * @param commentId 댓글 ID
     * @param status 댓글 상태
     * @return 댓글 Optional
     */
    @Query("SELECT c FROM Comment c " +
           "JOIN FETCH c.user " +
           "WHERE c.commentId = :commentId AND c.status = :status")
    Optional<Comment> findByIdAndStatusWithUser(
            @Param("commentId") Long commentId,
            @Param("status") CommentStatus status
    );

    /**
     * 게시글별 댓글 수 카운트 (ACTIVE만)
     * 통계 검증용
     *
     * @param postId 게시글 ID
     * @param status 댓글 상태 (ACTIVE)
     * @return 댓글 수
     */
    long countByPostPostIdAndStatus(Long postId, CommentStatus status);
}
