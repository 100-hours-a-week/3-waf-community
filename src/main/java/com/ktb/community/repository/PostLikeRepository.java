package com.ktb.community.repository;

import com.ktb.community.entity.PostLike;
import com.ktb.community.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 게시글 좋아요 Repository
 * FR-LIKE-001~003
 */
@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    /**
     * 좋아요 중복 확인
     */
    boolean existsByUserUserIdAndPostPostId(Long userId, Long postId);

    /**
     * 좋아요 여부 확인 (간단한 메서드명)
     */
    @Query("SELECT CASE WHEN COUNT(pl) > 0 THEN true ELSE false END " +
           "FROM PostLike pl WHERE pl.post.postId = :postId AND pl.user.userId = :userId")
    boolean existsByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    /**
     * 좋아요 조회 (취소용)
     */
    Optional<PostLike> findByUserUserIdAndPostPostId(Long userId, Long postId);

    /**
     * 좋아요한 게시글 목록 조회 (Fetch Join)
     */
    @Query("SELECT pl FROM PostLike pl " +
           "JOIN FETCH pl.post p " +
           "JOIN FETCH p.user " +
           "LEFT JOIN FETCH p.stats " +
           "WHERE pl.user.userId = :userId AND p.postStatus = :status " +
           "ORDER BY pl.createdAt DESC")
    Page<PostLike> findByUserIdWithPost(
            @Param("userId") Long userId,
            @Param("status") PostStatus status,
            Pageable pageable
    );

    /**
     * 좋아요 수 카운트 (통계 검증용)
     */
    long countByPostPostId(Long postId);
}
