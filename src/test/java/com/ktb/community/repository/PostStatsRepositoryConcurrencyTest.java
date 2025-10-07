package com.ktb.community.repository;

import com.ktb.community.entity.Post;
import com.ktb.community.entity.PostStats;
import com.ktb.community.entity.User;
import com.ktb.community.enums.PostStatus;
import com.ktb.community.enums.UserRole;
import com.ktb.community.enums.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostStatsRepository 동시성 테스트
 * @SpringBootTest 사용으로 트랜잭션 격리 및 실제 동시성 검증
 */
@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED) // 트랜잭션 비활성화
@DisplayName("PostStatsRepository 동시성 테스트")
class PostStatsRepositoryConcurrencyTest {

    @Autowired
    private PostStatsRepository postStatsRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private Post testPost;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        User user = User.builder()
                .email("concurrency@example.com")
                .passwordHash("hashedPassword")
                .nickname("concurrencyuser")
                .role(UserRole.USER)
                .userStatus(UserStatus.ACTIVE)
                .build();
        user = userRepository.save(user);

        // 테스트 게시글 생성
        testPost = Post.builder()
                .postTitle("Concurrency Test Post")
                .postContent("Test Content")
                .postStatus(PostStatus.ACTIVE)
                .user(user)
                .build();
        testPost = postRepository.save(testPost);

        // 테스트 통계 생성 (초기값 0)
        PostStats testStats = PostStats.builder()
                .post(testPost)
                .likeCount(0)
                .commentCount(0)
                .viewCount(0)
                .build();
        postStatsRepository.save(testStats);
    }

    @AfterEach
    void tearDown() {
        // 수동 정리
        postStatsRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("좋아요 수 동시 증가 (10 스레드) - 원자적 UPDATE 검증")
    void incrementLikeCount_Concurrency_Success() throws Exception {
        // Given
        Long postId = testPost.getPostId();
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When: 10개 스레드가 동시에 좋아요 수 증가
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    postStatsRepository.incrementLikeCount(postId);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드 완료 대기 (최대 5초)
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();

        // Then: 좋아요 수가 정확히 10이어야 함 (Race Condition 없음)
        PostStats result = postStatsRepository.findById(postId).orElseThrow();
        assertThat(result.getLikeCount()).isEqualTo(10);
    }
}
