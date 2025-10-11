package com.ktb.community.service;

import com.ktb.community.dto.request.PostCreateRequest;
import com.ktb.community.dto.request.PostUpdateRequest;
import com.ktb.community.dto.response.PostResponse;
import com.ktb.community.entity.Post;
import com.ktb.community.entity.PostStats;
import com.ktb.community.entity.User;
import com.ktb.community.enums.ErrorCode;
import com.ktb.community.enums.PostStatus;
import com.ktb.community.enums.UserRole;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.repository.PostRepository;
import com.ktb.community.repository.PostStatsRepository;
import com.ktb.community.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
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

/**
 * PostService 단위 테스트
 * PRD.md FR-POST-001~005 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostService 테스트")
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostStatsRepository postStatsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("게시글 작성 성공")
    void createPost_Success() {
        // Given
        Long userId = 1L;
        PostCreateRequest request = PostCreateRequest.builder()
                .title("Test Title")
                .content("Test Content")
                .build();

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encoded")
                .nickname("testnick")
                .role(UserRole.USER)
                .build();

        Post savedPost = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .status(PostStatus.ACTIVE)
                .user(user)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        when(postStatsRepository.save(any(PostStats.class))).thenReturn(PostStats.builder().post(savedPost).build());

        // When
        PostResponse response = postService.createPost(request, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo(request.getTitle());
        assertThat(response.getContent()).isEqualTo(request.getContent());
        verify(postRepository, times(1)).save(any(Post.class));
        verify(postStatsRepository, times(1)).save(any(PostStats.class));
    }

    @Test
    @DisplayName("게시글 작성 실패 - 사용자 없음")
    void createPost_UserNotFound_ThrowsException() {
        // Given
        Long userId = 999L;
        PostCreateRequest request = PostCreateRequest.builder()
                .title("Test Title")
                .content("Test Content")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postService.createPost(request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User not found");

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 목록 조회 성공")
    void getPosts_Success() {
        // Given
        int offset = 0;
        int limit = 10;
        String sort = "latest";

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encoded")
                .nickname("testnick")
                .role(UserRole.USER)
                .build();

        Post post = Post.builder()
                .title("Test Title")
                .content("Test Content")
                .status(PostStatus.ACTIVE)
                .user(user)
                .build();

        Page<Post> postPage = new PageImpl<>(List.of(post));

        when(postRepository.findByStatusWithUserAndStats(eq(PostStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(postPage);

        // When
        Map<String, Object> result = postService.getPosts(offset, limit, sort);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("posts");
        assertThat(result).containsKey("pagination");

        @SuppressWarnings("unchecked")
        List<PostResponse> posts = (List<PostResponse>) result.get("posts");
        assertThat(posts).hasSize(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> pagination = (Map<String, Object>) result.get("pagination");
        assertThat(pagination.get("total_count")).isEqualTo(1L);

        verify(postRepository, times(1)).findByStatusWithUserAndStats(eq(PostStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    @DisplayName("게시글 상세 조회 성공 - 조회수 증가")
    void getPostDetail_Success() {
        // Given
        Long postId = 1L;

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encoded")
                .nickname("testnick")
                .role(UserRole.USER)
                .build();

        PostStats stats = PostStats.builder()
                .build();
        ReflectionTestUtils.setField(stats, "postId", postId);
        ReflectionTestUtils.setField(stats, "viewCount", 5);

        Post post = Post.builder()
                .title("Test Title")
                .content("Test Content")
                .status(PostStatus.ACTIVE)
                .user(user)
                .build();
        // Post에 stats 미리 연결 (FETCH JOIN 시뮬레이션)
        post.updateStats(stats);

        when(postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE))
                .thenReturn(Optional.of(post));
        when(postStatsRepository.incrementViewCount(postId)).thenReturn(1);

        // When
        PostResponse response = postService.getPostDetail(postId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Test Title");
        verify(postStatsRepository, times(1)).incrementViewCount(postId);
    }

    @Test
    @DisplayName("게시글 상세 조회 실패 - 게시글 없음")
    void getPostDetail_PostNotFound_ThrowsException() {
        // Given
        Long postId = 999L;
        when(postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postService.getPostDetail(postId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Post not found");

        verify(postStatsRepository, never()).incrementViewCount(anyLong());
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void updatePost_Success() {
        // Given
        Long postId = 1L;
        Long userId = 1L;
        PostUpdateRequest request = PostUpdateRequest.builder()
                .title("Updated Title")
                .content("Updated Content")
                .build();

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encoded")
                .nickname("testnick")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "userId", 1L);

        Post post = Post.builder()
                .title("Original Title")
                .content("Original Content")
                .status(PostStatus.ACTIVE)
                .user(user)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // When
        PostResponse response = postService.updatePost(postId, request, userId);

        // Then
        assertThat(response).isNotNull();
        verify(postRepository, times(1)).findById(postId);
    }

    @Test
    @DisplayName("게시글 수정 실패 - 게시글 없음")
    void updatePost_PostNotFound_ThrowsException() {
        // Given
        Long postId = 999L;
        Long userId = 1L;
        PostUpdateRequest request = PostUpdateRequest.builder()
                .title("Updated Title")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postService.updatePost(postId, request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Post not found");
    }

    @Test
    @DisplayName("게시글 수정 실패 - 권한 없음 (다른 사용자)")
    void updatePost_Unauthorized_ThrowsException() {
        // Given
        Long postId = 1L;
        Long ownerId = 1L;
        Long requesterId = 2L;  // 다른 사용자
        PostUpdateRequest request = PostUpdateRequest.builder()
                .title("Updated Title")
                .build();

        User owner = User.builder()
                .email("owner@example.com")
                .passwordHash("encoded")
                .nickname("owner")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(owner, "userId", 1L);

        Post post = Post.builder()
                .title("Original Title")
                .content("Original Content")
                .status(PostStatus.ACTIVE)
                .user(owner)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // When & Then
        assertThatThrownBy(() -> postService.updatePost(postId, request, requesterId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Not authorized");
    }

    @Test
    @DisplayName("게시글 수정 실패 - 업데이트 필드 없음")
    void updatePost_NoFields_ThrowsException() {
        // Given
        Long postId = 1L;
        Long userId = 1L;
        PostUpdateRequest request = PostUpdateRequest.builder().build();  // 모든 필드 null

        // When & Then
        assertThatThrownBy(() -> postService.updatePost(postId, request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("At least one field must be provided");

        verify(postRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("게시글 삭제 성공 - Soft Delete")
    void deletePost_Success() {
        // Given
        Long postId = 1L;
        Long userId = 1L;

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encoded")
                .nickname("testnick")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "userId", 1L);

        Post post = Post.builder()
                .title("Test Title")
                .content("Test Content")
                .status(PostStatus.ACTIVE)
                .user(user)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // When
        postService.deletePost(postId, userId);

        // Then
        verify(postRepository, times(1)).findById(postId);
        // Soft Delete이므로 실제 삭제 메서드는 호출되지 않음
        verify(postRepository, never()).delete(any(Post.class));
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 권한 없음")
    void deletePost_Unauthorized_ThrowsException() {
        // Given
        Long postId = 1L;
        Long ownerId = 1L;
        Long requesterId = 2L;

        User owner = User.builder()
                .email("owner@example.com")
                .passwordHash("encoded")
                .nickname("owner")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(owner, "userId", 1L);

        Post post = Post.builder()
                .title("Test Title")
                .content("Test Content")
                .status(PostStatus.ACTIVE)
                .user(owner)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // When & Then
        assertThatThrownBy(() -> postService.deletePost(postId, requesterId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Not authorized");
    }
}
