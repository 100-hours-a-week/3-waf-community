package com.ktb.community.service;

import com.ktb.community.dto.request.PostCreateRequest;
import com.ktb.community.dto.request.PostUpdateRequest;
import com.ktb.community.dto.response.PostResponse;
import com.ktb.community.entity.Image;
import com.ktb.community.entity.Post;
import com.ktb.community.entity.PostImage;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ktb.community.enums.UserStatus;

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

    @Mock
    private com.ktb.community.repository.ImageRepository imageRepository;

    @Mock
    private com.ktb.community.repository.PostImageRepository postImageRepository;

    @Mock
    private com.ktb.community.repository.PostLikeRepository postLikeRepository;

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

        when(userRepository.findByUserIdAndUserStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
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

        when(userRepository.findByUserIdAndUserStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postService.createPost(request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User not found");

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 목록 조회 성공 - Cursor (latest)")
    void getPosts_Cursor_Success() {
        // Given
        Long cursor = null;
        int limit = 10;
        String sort = "latest";

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encoded")
                .nickname("testnick")
                .role(UserRole.USER)
                .build();

        Post post1 = Post.builder()
                .title("Test Title 1")
                .content("Test Content 1")
                .status(PostStatus.ACTIVE)
                .user(user)
                .build();
        // postId 설정 (reflection 사용)
        ReflectionTestUtils.setField(post1, "postId", 100L);

        Post post2 = Post.builder()
                .title("Test Title 2")
                .content("Test Content 2")
                .status(PostStatus.ACTIVE)
                .user(user)
                .build();
        ReflectionTestUtils.setField(post2, "postId", 99L);

        when(postRepository.findByStatusWithoutCursor(eq(PostStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(List.of(post1, post2));

        // When
        Map<String, Object> result = postService.getPosts(cursor, null, limit, sort);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("posts");
        assertThat(result).containsKey("nextCursor");
        assertThat(result).containsKey("hasMore");

        @SuppressWarnings("unchecked")
        List<PostResponse> posts = (List<PostResponse>) result.get("posts");
        assertThat(posts).hasSize(2);

        assertThat(result.get("nextCursor")).isNull();
        assertThat(result.get("hasMore")).isEqualTo(false);

        verify(postRepository, times(1)).findByStatusWithoutCursor(eq(PostStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    @DisplayName("게시글 목록 조회 성공 - Offset (likes)")
    void getPosts_Offset_Success() {
        // Given
        int offset = 0;
        int limit = 10;
        String sort = "likes";

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
        Map<String, Object> result = postService.getPosts(null, offset, limit, sort);

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
        Long userId = 100L;

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
        when(postLikeRepository.existsByPostIdAndUserId(postId, userId)).thenReturn(true);

        // When
        PostResponse response = postService.getPostDetail(postId, userId);

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
        assertThatThrownBy(() -> postService.getPostDetail(postId, null))
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

        when(postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE)).thenReturn(Optional.of(post));

        // When
        PostResponse response = postService.updatePost(postId, request, userId);

        // Then
        assertThat(response).isNotNull();
        verify(postRepository, times(1)).findByIdWithUserAndStats(postId, PostStatus.ACTIVE);
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

        when(postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE)).thenReturn(Optional.empty());

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

        when(postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE)).thenReturn(Optional.of(post));

        // When & Then
        assertThatThrownBy(() -> postService.updatePost(postId, request, requesterId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Not authorized");
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

        when(postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE)).thenReturn(Optional.of(post));

        // When
        postService.deletePost(postId, userId);

        // Then
        verify(postRepository, times(1)).findByIdWithUserAndStats(postId, PostStatus.ACTIVE);
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

        when(postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE)).thenReturn(Optional.of(post));

        // When & Then
        assertThatThrownBy(() -> postService.deletePost(postId, requesterId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Not authorized");
    }

    @Test
    @DisplayName("게시글 작성 실패 - INACTIVE 사용자")
    void createPost_WithInactiveUser_ThrowsException() {
        // Given
        Long inactiveUserId = 999L;
        PostCreateRequest request = new PostCreateRequest("Title", "Content", null);

        when(userRepository.findByUserIdAndUserStatus(inactiveUserId, UserStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postService.createPost(request, inactiveUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND)
                .hasMessageContaining("User not found or inactive");
    }

    @Test
    @DisplayName("게시글 수정 - 이미지 제거 시 TTL 복원")
    void updatePost_RemoveImage_ShouldRestoreTTL() {
        // Given
        Long postId = 1L;
        Long userId = 1L;

        // User 생성
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encoded")
                .nickname("testuser")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "userId", userId);

        // Post 생성
        Post post = Post.builder()
                .title("Test Title")
                .content("Test Content")
                .status(PostStatus.ACTIVE)
                .user(user)
                .build();
        ReflectionTestUtils.setField(post, "postId", postId);

        // PostStats 생성
        PostStats stats = PostStats.builder()
                .post(post)
                .build();
        ReflectionTestUtils.setField(stats, "postId", postId);
        post.updateStats(stats);

        // Image 생성 (expires_at = NULL, 영구 보존 상태)
        Image image = Image.builder()
                .imageUrl("https://s3.amazonaws.com/test.jpg")
                .fileSize(1024)
                .originalFilename("test.jpg")
                .expiresAt(null)  // 영구 보존
                .build();
        ReflectionTestUtils.setField(image, "imageId", 10L);

        // PostImage 생성
        PostImage postImage = PostImage.builder()
                .post(post)
                .image(image)
                .displayOrder(1)
                .build();

        // Request: removeImage = true
        PostUpdateRequest request = PostUpdateRequest.builder()
                .removeImage(true)
                .build();

        // Mocking
        when(postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE))
                .thenReturn(Optional.of(post));
        when(postImageRepository.findByPostIdWithImage(postId))
                .thenReturn(List.of(postImage));
        when(postImageRepository.deleteByPostId(postId)).thenReturn(1);

        // When
        PostResponse response = postService.updatePost(postId, request, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(image.getExpiresAt()).isNotNull();  // TTL 복원 확인
        assertThat(image.getExpiresAt()).isAfter(LocalDateTime.now());  // 미래 시간

        verify(postImageRepository).findByPostIdWithImage(postId);
        verify(postImageRepository).deleteByPostId(postId);
    }

    @Test
    @DisplayName("게시글 수정 - 이미지 교체 시 기존 이미지 TTL 복원")
    void updatePost_ReplaceImage_ShouldRestoreOldImageTTL() {
        // Given
        Long postId = 1L;
        Long userId = 1L;
        Long oldImageId = 10L;
        Long newImageId = 20L;

        // User 생성
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encoded")
                .nickname("testuser")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "userId", userId);

        // Post 생성
        Post post = Post.builder()
                .title("Test Title")
                .content("Test Content")
                .status(PostStatus.ACTIVE)
                .user(user)
                .build();
        ReflectionTestUtils.setField(post, "postId", postId);

        // PostStats 생성
        PostStats stats = PostStats.builder()
                .post(post)
                .build();
        ReflectionTestUtils.setField(stats, "postId", postId);
        post.updateStats(stats);

        // 기존 Image (expires_at = NULL)
        Image oldImage = Image.builder()
                .imageUrl("https://s3.amazonaws.com/old.jpg")
                .fileSize(1024)
                .originalFilename("old.jpg")
                .expiresAt(null)
                .build();
        ReflectionTestUtils.setField(oldImage, "imageId", oldImageId);

        // PostImage (기존)
        PostImage oldPostImage = PostImage.builder()
                .post(post)
                .image(oldImage)
                .displayOrder(1)
                .build();

        // 새 Image (expires_at = now + 1h, 업로드 직후 상태)
        Image newImage = Image.builder()
                .imageUrl("https://s3.amazonaws.com/new.jpg")
                .fileSize(2048)
                .originalFilename("new.jpg")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        ReflectionTestUtils.setField(newImage, "imageId", newImageId);

        // Request: imageId = 20 (교체)
        PostUpdateRequest request = PostUpdateRequest.builder()
                .imageId(newImageId)
                .build();

        // Mocking
        when(postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE))
                .thenReturn(Optional.of(post));
        when(postImageRepository.findByPostIdWithImage(postId))
                .thenReturn(List.of(oldPostImage));
        when(postImageRepository.deleteByPostId(postId)).thenReturn(1);
        when(imageRepository.findById(newImageId)).thenReturn(Optional.of(newImage));
        when(postImageRepository.save(any(PostImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PostResponse response = postService.updatePost(postId, request, userId);

        // Then
        assertThat(response).isNotNull();

        // 기존 이미지: TTL 복원 확인
        assertThat(oldImage.getExpiresAt()).isNotNull();
        assertThat(oldImage.getExpiresAt()).isAfter(LocalDateTime.now());

        // 새 이미지: 영구 보존 (expires_at = NULL)
        assertThat(newImage.getExpiresAt()).isNull();

        verify(postImageRepository).findByPostIdWithImage(postId);
        verify(postImageRepository).deleteByPostId(postId);
        verify(imageRepository).findById(newImageId);
        verify(postImageRepository).save(any(PostImage.class));
    }

    @Test
    @DisplayName("게시글 수정 - 이미지 변경 없음")
    void updatePost_NoImageChange_ShouldKeepImage() {
        // Given
        Long postId = 1L;
        Long userId = 1L;

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encoded")
                .nickname("testuser")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "userId", userId);

        Post post = Post.builder()
                .title("Test Title")
                .content("Test Content")
                .status(PostStatus.ACTIVE)
                .user(user)
                .build();
        ReflectionTestUtils.setField(post, "postId", postId);

        PostStats stats = PostStats.builder()
                .post(post)
                .build();
        ReflectionTestUtils.setField(stats, "postId", postId);
        post.updateStats(stats);

        // Request: title만 변경 (이미지 관련 필드 없음)
        PostUpdateRequest request = PostUpdateRequest.builder()
                .title("Updated Title")
                .build();

        when(postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE))
                .thenReturn(Optional.of(post));

        // When
        PostResponse response = postService.updatePost(postId, request, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Updated Title");

        // 이미지 관련 메서드 호출 안 됨
        verify(postImageRepository, never()).findByPostIdWithImage(anyLong());
        verify(postImageRepository, never()).deleteByPostId(anyLong());
        verify(imageRepository, never()).findById(anyLong());
    }
}
