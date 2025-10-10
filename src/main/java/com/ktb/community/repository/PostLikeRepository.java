package com.ktb.community.repository;

import com.ktb.community.entity.PostLike;
import com.ktb.community.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 게시글 좋아요 Repository
 * PRD.md FR-LIKE-001~003 참조
 */
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    /**
     * 좋아요 중복 확인
     * DDL의 UNIQUE 제약조건 (user_id, post_id) 확인용
     *
     * @param userId 사용자 ID
     * @param postId 게시글 ID
     * @return 좋아요 존재 여부
     */
    boolean existsByUserUserIdAndPostPostId(Long userId, Long postId);

    /**
     * 좋아요 조회 (취소 시 삭제용)
     *
     * @param userId 사용자 ID
     * @param postId 게시글 ID
     * @return PostLike Optional
     */
    Optional<PostLike> findByUserUserIdAndPostPostId(Long userId, Long postId);

    /**
     * 사용자가 좋아요한 게시글 목록 조회 (ACTIVE만, 페이지네이션, N+1 방지)
     * Fetch Join으로 post, user, stats 정보 함께 조회
     *
     * @param userId 사용자 ID
     * @param status 게시글 상태 (ACTIVE)
     * @param pageable 페이지 정보 (offset, limit)
     * @return 좋아요 페이지 (게시글 정보 포함)
     */
    @Query("SELECT pl FROM PostLike pl " +
           "JOIN FETCH pl.post p " +
           "JOIN FETCH p.user " +
           "LEFT JOIN FETCH p.stats " +
           "WHERE pl.user.userId = :userId AND p.status = :status " +
           "ORDER BY pl.createdAt DESC")
    Page<PostLike> findByUserIdWithPost(
            @Param("userId") Long userId,
            @Param("status") PostStatus status,
            Pageable pageable
    );

    /**
     * 게시글별 좋아요 수 카운트 (통계 검증용)
     *
     * @param postId 게시글 ID
     * @return 좋아요 수
     */
    long countByPostPostId(Long postId);
}
