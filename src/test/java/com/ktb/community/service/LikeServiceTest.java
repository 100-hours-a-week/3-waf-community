package com.ktb.community.service;

import com.ktb.community.entity.Post;
import com.ktb.community.entity.PostLike;
import com.ktb.community.entity.PostStats;
import com.ktb.community.entity.User;
import com.ktb.community.enums.PostStatus;
import com.ktb.community.enums.UserRole;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.repository.PostLikeRepository;
import com.ktb.community.repository.PostRepository;
import com.ktb.community.repository.PostStatsRepository;
import com.ktb.community.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ktb.community.enums.UserStatus;

/**
 * LikeService 단위 테스트
 * PRD.md FR-LIKE-001~003 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LikeService 테스트")
class LikeServiceTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostStatsRepository postStatsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LikeService likeService;

    @Test
    @DisplayName("좋아요 추가 성공 - likeCount 증가")
    void addLike_Success() {
        // Given
        Long postId = 1L;
        Long userId = 1L;

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encoded")
                .nickname("testnick")
                .role(UserRole.USER)
                .build();

        Post post = Post.builder()
                .title("Test Post")
                .content("Test Content")
                .status(PostStatus.ACTIVE)
                .user(user)
                .build();

        PostStats stats = PostStats.builder()
                .post(post)
                .build();

        when(postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE))
                .thenReturn(Optional.of(post));
        when(userRepository.findByUserIdAndUserStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(postLikeRepository.existsByUserUserIdAndPostPostId(userId, postId)).thenReturn(false);
        when(postLikeRepository.save(any(PostLike.class))).thenReturn(null);
        when(postStatsRepository.incrementLikeCount(postId)).thenReturn(1);
        when(postStatsRepository.findById(postId)).thenReturn(Optional.of(stats));

        // When
        Map<String, Integer> result = likeService.addLike(postId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("like_count");
        assertThat(result.get("like_count")).isEqualTo(0);  // PostStats의 초기값

        verify(postLikeRepository, times(1)).save(any(PostLike.class));
        verify(postStatsRepository, times(1)).incrementLikeCount(postId);
    }

    @Test
    @DisplayName("좋아요 추가 실패 - 게시글 없음")
    void addLike_PostNotFound_ThrowsException() {
        // Given
        Long postId = 999L;
        Long userId = 1L;

        when(postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> likeService.addLike(postId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Post not found");

        verify(postLikeRepository, never()).save(any(PostLike.class));
        verify(postStatsRepository, never()).incrementLikeCount(anyLong());
    }

    @Test
    @DisplayName("좋아요 추가 실패 - 사용자 없음")
    void addLike_UserNotFound_ThrowsException() {
        // Given
        Long postId = 1L;
        Long userId = 999L;

        User postAuthor = User.builder()
                .email("author@example.com")
                .passwordHash("encoded")
                .nickname("author")
                .role(UserRole.USER)
                .build();

        Post post = Post.builder()
                .title("Test Post")
                .content("Test Content")
                .status(PostStatus.ACTIVE)
                .user(postAuthor)
                .build();

        when(postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE))
                .thenReturn(Optional.of(post));
        when(userRepository.findByUserIdAndUserStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> likeService.addLike(postId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User not found");

        verify(postLikeRepository, never()).save(any(PostLike.class));
    }

    @Test
    @DisplayName("좋아요 추가 실패 - 중복 좋아요")
    void addLike_AlreadyLiked_ThrowsException() {
        // Given
        Long postId = 1L;
        Long userId = 1L;

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encoded")
                .nickname("testnick")
                .role(UserRole.USER)
                .build();

        Post post = Post.builder()
                .title("Test Post")
                .content("Test Content")
                .status(PostStatus.ACTIVE)
                .user(user)
                .build();

        when(postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE))
                .thenReturn(Optional.of(post));
        when(userRepository.findByUserIdAndUserStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(postLikeRepository.existsByUserUserIdAndPostPostId(userId, postId)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> likeService.addLike(postId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already liked");

        verify(postLikeRepository, never()).save(any(PostLike.class));
        verify(postStatsRepository, never()).incrementLikeCount(anyLong());
    }

    @Test
    @DisplayName("좋아요 취소 성공 - likeCount 감소")
    void removeLike_Success() {
        // Given
        Long postId = 1L;
        Long userId = 1L;

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encoded")
                .nickname("testnick")
                .role(UserRole.USER)
                .build();

        Post post = Post.builder()
                .title("Test Post")
                .content("Test Content")
                .status(PostStatus.ACTIVE)
                .user(user)
                .build();

        PostLike postLike = PostLike.builder()
                .user(user)
                .post(post)
                .build();

        PostStats stats = PostStats.builder()
                .post(post)
                .build();

        when(postRepository.existsByPostIdAndPostStatus(postId, PostStatus.ACTIVE)).thenReturn(true);
        when(postLikeRepository.findByUserUserIdAndPostPostId(userId, postId))
                .thenReturn(Optional.of(postLike));
        when(postStatsRepository.decrementLikeCount(postId)).thenReturn(1);
        when(postStatsRepository.findById(postId)).thenReturn(Optional.of(stats));

        // When
        Map<String, Integer> result = likeService.removeLike(postId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("like_count");

        verify(postLikeRepository, times(1)).delete(postLike);
        verify(postStatsRepository, times(1)).decrementLikeCount(postId);
    }

    @Test
    @DisplayName("좋아요 취소 실패 - 게시글 없음")
    void removeLike_PostNotFound_ThrowsException() {
        // Given
        Long postId = 999L;
        Long userId = 1L;

        when(postRepository.existsByPostIdAndPostStatus(postId, PostStatus.ACTIVE)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> likeService.removeLike(postId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Post not found");

        verify(postLikeRepository, never()).delete(any(PostLike.class));
        verify(postStatsRepository, never()).decrementLikeCount(anyLong());
    }

    @Test
    @DisplayName("좋아요 취소 실패 - 좋아요 없음")
    void removeLike_LikeNotFound_ThrowsException() {
        // Given
        Long postId = 1L;
        Long userId = 1L;

        when(postRepository.existsByPostIdAndPostStatus(postId, PostStatus.ACTIVE)).thenReturn(true);
        when(postLikeRepository.findByUserUserIdAndPostPostId(userId, postId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> likeService.removeLike(postId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Like not found");

        verify(postLikeRepository, never()).delete(any(PostLike.class));
        verify(postStatsRepository, never()).decrementLikeCount(anyLong());
    }

    @Test
    @DisplayName("좋아요한 게시글 목록 조회 성공")
    void getLikedPosts_Success() {
        // Given
        Long userId = 1L;
        int offset = 0;
        int limit = 10;

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encoded")
                .nickname("testnick")
                .role(UserRole.USER)
                .build();

        Post post = Post.builder()
                .title("Test Post")
                .content("Test Content")
                .status(PostStatus.ACTIVE)
                .user(user)
                .build();

        PostLike postLike = PostLike.builder()
                .user(user)
                .post(post)
                .build();

        Page<PostLike> likePage = new PageImpl<>(List.of(postLike));

        when(userRepository.existsById(userId)).thenReturn(true);
        when(postLikeRepository.findByUserIdWithPost(eq(userId), eq(PostStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(likePage);

        // When
        Map<String, Object> result = likeService.getLikedPosts(userId, offset, limit);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("posts");
        assertThat(result).containsKey("pagination");

        @SuppressWarnings("unchecked")
        Map<String, Object> pagination = (Map<String, Object>) result.get("pagination");
        assertThat(pagination.get("total_count")).isEqualTo(1L);

        verify(postLikeRepository, times(1))
                .findByUserIdWithPost(eq(userId), eq(PostStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    @DisplayName("좋아요한 게시글 목록 조회 실패 - 사용자 없음")
    void getLikedPosts_UserNotFound_ThrowsException() {
        // Given
        Long userId = 999L;
        int offset = 0;
        int limit = 10;

        when(userRepository.existsById(userId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> likeService.getLikedPosts(userId, offset, limit))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User not found");

        verify(postLikeRepository, never())
                .findByUserIdWithPost(anyLong(), any(PostStatus.class), any(Pageable.class));
    }
}
