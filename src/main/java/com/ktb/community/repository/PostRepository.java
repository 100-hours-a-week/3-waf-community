package com.ktb.community.repository;

import com.ktb.community.entity.Post;
import com.ktb.community.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 게시글 Repository
 * FR-POST-001~005
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 게시글 목록 조회 (Fetch Join)
     */
    @Query("SELECT p FROM Post p " +
           "JOIN FETCH p.user " +
           "LEFT JOIN FETCH p.stats " +
           "WHERE p.postStatus = :status")
    Page<Post> findByStatusWithUserAndStats(@Param("status") PostStatus status, Pageable pageable);

    /**
     * 게시글 목록 조회 - Cursor 기반 (첫 페이지)
     * - latest 정렬 전용
     * - cursor=null일 때 사용
     */
    @Query("SELECT p FROM Post p " +
           "JOIN FETCH p.user " +
           "LEFT JOIN FETCH p.stats " +
           "WHERE p.postStatus = :status " +
           "ORDER BY p.postId DESC")
    java.util.List<Post> findByStatusWithoutCursor(@Param("status") PostStatus status, Pageable pageable);

    /**
     * 게시글 목록 조회 - Cursor 기반 (후속 페이지)
     * - latest 정렬 전용
     * - cursor 이후 데이터 조회
     */
    @Query("SELECT p FROM Post p " +
           "JOIN FETCH p.user " +
           "LEFT JOIN FETCH p.stats " +
           "WHERE p.postStatus = :status AND p.postId < :cursor " +
           "ORDER BY p.postId DESC")
    java.util.List<Post> findByStatusWithCursor(
            @Param("status") PostStatus status,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    /**
     * 게시글 상세 조회 (Fetch Join)
     */
    @Query("SELECT p FROM Post p " +
           "JOIN FETCH p.user " +
           "LEFT JOIN FETCH p.stats " +
           "WHERE p.postId = :postId AND p.postStatus = :status")
    Optional<Post> findByIdWithUserAndStats(@Param("postId") Long postId, @Param("status") PostStatus status);

    /**
     * 상태별 존재 확인
     */
    boolean existsByPostIdAndPostStatus(Long postId, PostStatus status);

    /**
     * 상태별 게시글 수 조회
     */
    long countByPostStatus(PostStatus status);
}
