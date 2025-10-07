package com.ktb.community.repository;

import com.ktb.community.entity.Post;
import com.ktb.community.entity.PostStats;
import com.ktb.community.entity.User;
import com.ktb.community.enums.PostStatus;
import com.ktb.community.enums.UserRole;
import com.ktb.community.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostStatsRepository 단위 테스트
 * 동시성 테스트는 별도 클래스(PostStatsRepositoryConcurrencyTest)에서 수행
 */
@DataJpaTest
@DisplayName("PostStatsRepository 테스트")
class PostStatsRepositoryTest {

    @Autowired
    private PostStatsRepository postStatsRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Post testPost;
    private PostStats testStats;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .nickname("testuser")
                .role(UserRole.USER)
                .userStatus(UserStatus.ACTIVE)
                .build();
        entityManager.persist(user);

        // 테스트 게시글 생성
        testPost = Post.builder()
                .postTitle("Test Post")
                .postContent("Test Content")
                .postStatus(PostStatus.ACTIVE)
                .user(user)
                .build();
        entityManager.persist(testPost);

        // 테스트 통계 생성 (초기값 0)
        testStats = PostStats.builder()
                .post(testPost)
                .likeCount(0)
                .commentCount(0)
                .viewCount(0)
                .build();
        entityManager.persist(testStats);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("좋아요 수 증가 - 원자적 UPDATE")
    void incrementLikeCount_Success() {
        // Given: likeCount = 0

        // When
        int updatedRows = postStatsRepository.incrementLikeCount(testPost.getPostId());
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(updatedRows).isEqualTo(1);

        PostStats result = postStatsRepository.findById(testPost.getPostId()).orElseThrow();
        assertThat(result.getLikeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("좋아요 수 감소 - 원자적 UPDATE")
    void decrementLikeCount_Success() {
        // Given: likeCount = 5로 설정
        testStats.setLikeCount(5);
        entityManager.merge(testStats);
        entityManager.flush();
        entityManager.clear();

        // When
        int updatedRows = postStatsRepository.decrementLikeCount(testPost.getPostId());
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(updatedRows).isEqualTo(1);

        PostStats result = postStatsRepository.findById(testPost.getPostId()).orElseThrow();
        assertThat(result.getLikeCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("댓글 수 증가 - 원자적 UPDATE")
    void incrementCommentCount_Success() {
        // Given: commentCount = 0

        // When
        int updatedRows = postStatsRepository.incrementCommentCount(testPost.getPostId());
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(updatedRows).isEqualTo(1);

        PostStats result = postStatsRepository.findById(testPost.getPostId()).orElseThrow();
        assertThat(result.getCommentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("댓글 수 감소 - 원자적 UPDATE")
    void decrementCommentCount_Success() {
        // Given: commentCount = 3으로 설정
        testStats.setCommentCount(3);
        entityManager.merge(testStats);
        entityManager.flush();
        entityManager.clear();

        // When
        int updatedRows = postStatsRepository.decrementCommentCount(testPost.getPostId());
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(updatedRows).isEqualTo(1);

        PostStats result = postStatsRepository.findById(testPost.getPostId()).orElseThrow();
        assertThat(result.getCommentCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("조회수 증가 - 원자적 UPDATE")
    void incrementViewCount_Success() {
        // Given: viewCount = 10으로 설정
        testStats.setViewCount(10);
        entityManager.merge(testStats);
        entityManager.flush();
        entityManager.clear();

        // When
        int updatedRows = postStatsRepository.incrementViewCount(testPost.getPostId());
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(updatedRows).isEqualTo(1);

        PostStats result = postStatsRepository.findById(testPost.getPostId()).orElseThrow();
        assertThat(result.getViewCount()).isEqualTo(11);
    }
}
