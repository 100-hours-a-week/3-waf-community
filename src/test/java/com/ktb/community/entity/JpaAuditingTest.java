package com.ktb.community.entity;

import com.ktb.community.config.JpaAuditingConfig;
import com.ktb.community.enums.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * JPA Auditing 작동 검증 테스트
 * BaseTimeEntity, BaseCreatedTimeEntity의 자동 시간 관리 확인
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class JpaAuditingTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("User 엔티티 생성 시 createdAt, updatedAt 자동 설정")
    void user_createdAt_updatedAt_auto_set() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .nickname("테스터")
                .role(UserRole.USER)
                .build();

        // When
        entityManager.persist(user);
        entityManager.flush();

        // Then
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(user.getUpdatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Post 엔티티 생성 시 createdAt, updatedAt 자동 설정")
    void post_timestamps_auto_set() {
        // Given
        User user = User.builder()
                .email("author@example.com")
                .passwordHash("hashedPassword")
                .nickname("작성자")
                .role(UserRole.USER)
                .build();
        entityManager.persist(user);

        Post post = Post.builder()
                .postTitle("테스트 게시글")
                .postContent("내용")
                .user(user)
                .build();

        // When
        entityManager.persist(post);
        entityManager.flush();

        // Then
        assertThat(post.getCreatedAt()).isNotNull();
        assertThat(post.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Comment 엔티티 생성 시 createdAt, updatedAt 자동 설정")
    void comment_timestamps_auto_set() {
        // Given
        User user = User.builder()
                .email("user@example.com")
                .passwordHash("hashedPassword")
                .nickname("유저")
                .role(UserRole.USER)
                .build();
        entityManager.persist(user);

        Post post = Post.builder()
                .postTitle("게시글")
                .postContent("내용")
                .user(user)
                .build();
        entityManager.persist(post);

        Comment comment = Comment.builder()
                .commentContent("댓글 내용")
                .post(post)
                .user(user)
                .build();

        // When
        entityManager.persist(comment);
        entityManager.flush();

        // Then
        assertThat(comment.getCreatedAt()).isNotNull();
        assertThat(comment.getUpdatedAt()).isNotNull();
        assertThat(comment.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Image 엔티티(BaseCreatedTimeEntity) 생성 시 createdAt만 자동 설정")
    void image_createdAt_only() {
        // Given
        Image image = Image.builder()
                .imageUrl("https://example.com/image.jpg")
                .fileSize(1024)
                .originalFilename("test.jpg")
                .build();

        // When
        entityManager.persist(image);
        entityManager.flush();

        // Then
        assertThat(image.getCreatedAt()).isNotNull();
        assertThat(image.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }
}
