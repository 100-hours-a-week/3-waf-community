package com.ktb.community.repository;

import com.ktb.community.entity.PostImage;
import com.ktb.community.entity.PostImageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 게시글-이미지 브릿지 Repository
 * PRD.md FR-IMAGE-002 참조 (게시글 이미지 관리)
 */
public interface PostImageRepository extends JpaRepository<PostImage, PostImageId> {

    /**
     * 게시글의 이미지 목록 조회 (display_order 순서, N+1 방지)
     * Fetch Join으로 image 정보 함께 조회
     *
     * @param postId 게시글 ID
     * @return 이미지 목록 (display_order 오름차순)
     */
    @Query("SELECT pi FROM PostImage pi " +
           "JOIN FETCH pi.image " +
           "WHERE pi.post.postId = :postId " +
           "ORDER BY pi.displayOrder ASC")
    List<PostImage> findByPostIdWithImage(@Param("postId") Long postId);

    /**
     * 게시글의 모든 이미지 연결 삭제 (Bulk Delete)
     * 게시글 수정 시 기존 이미지 제거용
     *
     * @param postId 게시글 ID
     * @return 삭제된 행 수
     */
    @Modifying
    @Query("DELETE FROM PostImage pi WHERE pi.post.postId = :postId")
    int deleteByPostId(@Param("postId") Long postId);
}
