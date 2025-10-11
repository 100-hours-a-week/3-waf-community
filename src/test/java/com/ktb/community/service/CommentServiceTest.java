package com.ktb.community.service;

import com.ktb.community.dto.request.CommentCreateRequest;
import com.ktb.community.dto.request.CommentUpdateRequest;
import com.ktb.community.dto.response.CommentResponse;
import com.ktb.community.entity.Comment;
import com.ktb.community.entity.Post;
import com.ktb.community.entity.User;
import com.ktb.community.enums.CommentStatus;
import com.ktb.community.enums.PostStatus;
import com.ktb.community.enums.UserRole;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.repository.CommentRepository;
import com.ktb.community.repository.PostRepository;
import com.ktb.community.repository.PostStatsRepository;
import com.ktb.community.repository.UserRepository;
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
 * CommentService 단위 테스트
 * PRD.md FR-COMMENT-001~004 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService 테스트")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostStatsRepository postStatsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    @DisplayName("댓글 작성 성공 - commentCount 증가")
    void createComment_Success() {
        // Given
        Long postId = 1L;
        Long userId = 1L;
        CommentCreateRequest request = CommentCreateRequest.builder()
                .comment("Test Comment")
                .build();

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

        Comment savedComment = Comment.builder()
                .content(request.getComment())
                .status(CommentStatus.ACTIVE)
                .post(post)
                .user(user)
                .build();

        when(postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE))
                .thenReturn(Optional.of(post));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
        when(postStatsRepository.incrementCommentCount(postId)).thenReturn(1);

        // When
        CommentResponse response = commentService.createComment(postId, request, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo(request.getComment());
        verify(commentRepository, times(1)).save(any(Comment.class));
        verify(postStatsRepository, times(1)).incrementCommentCount(postId);
    }

    @Test
    @DisplayName("댓글 작성 실패 - 게시글 없음")
    void createComment_PostNotFound_ThrowsException() {
        // Given
        Long postId = 999L;
        Long userId = 1L;
        CommentCreateRequest request = CommentCreateRequest.builder()
                .comment("Test Comment")
                .build();

        when(postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(postId, request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Post not found");

        verify(commentRepository, never()).save(any(Comment.class));
        verify(postStatsRepository, never()).incrementCommentCount(anyLong());
    }

    @Test
    @DisplayName("댓글 작성 실패 - 사용자 없음")
    void createComment_UserNotFound_ThrowsException() {
        // Given
        Long postId = 1L;
        Long userId = 999L;
        CommentCreateRequest request = CommentCreateRequest.builder()
                .comment("Test Comment")
                .build();

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
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(postId, request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User not found");

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 목록 조회 성공")
    void getComments_Success() {
        // Given
        Long postId = 1L;
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

        Comment comment = Comment.builder()
                .content("Test Comment")
                .status(CommentStatus.ACTIVE)
                .post(post)
                .user(user)
                .build();

        Page<Comment> commentPage = new PageImpl<>(List.of(comment));

        when(postRepository.existsByPostIdAndPostStatus(postId, PostStatus.ACTIVE)).thenReturn(true);
        when(commentRepository.findByPostIdAndStatusWithUser(eq(postId), eq(CommentStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(commentPage);

        // When
        Map<String, Object> result = commentService.getComments(postId, offset, limit);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("comments");
        assertThat(result).containsKey("pagination");

        @SuppressWarnings("unchecked")
        List<CommentResponse> comments = (List<CommentResponse>) result.get("comments");
        assertThat(comments).hasSize(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> pagination = (Map<String, Object>) result.get("pagination");
        assertThat(pagination.get("total_count")).isEqualTo(1L);

        verify(commentRepository, times(1))
                .findByPostIdAndStatusWithUser(eq(postId), eq(CommentStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    @DisplayName("댓글 목록 조회 실패 - 게시글 없음")
    void getComments_PostNotFound_ThrowsException() {
        // Given
        Long postId = 999L;
        int offset = 0;
        int limit = 10;

        when(postRepository.existsByPostIdAndPostStatus(postId, PostStatus.ACTIVE)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> commentService.getComments(postId, offset, limit))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Post not found");

        verify(commentRepository, never())
                .findByPostIdAndStatusWithUser(anyLong(), any(CommentStatus.class), any(Pageable.class));
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void updateComment_Success() {
        // Given
        Long commentId = 1L;
        Long userId = 1L;
        CommentUpdateRequest request = CommentUpdateRequest.builder()
                .comment("Updated Comment")
                .build();

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encoded")
                .nickname("testnick")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "userId", 1L);

        Post post = Post.builder()
                .title("Test Post")
                .content("Test Content")
                .status(PostStatus.ACTIVE)
                .user(user)
                .build();

        Comment comment = Comment.builder()
                .content("Original Comment")
                .status(CommentStatus.ACTIVE)
                .post(post)
                .user(user)
                .build();

        when(commentRepository.findByIdAndStatusWithUser(commentId, CommentStatus.ACTIVE))
                .thenReturn(Optional.of(comment));

        // When
        CommentResponse response = commentService.updateComment(commentId, request, userId);

        // Then
        assertThat(response).isNotNull();
        verify(commentRepository, times(1)).findByIdAndStatusWithUser(commentId, CommentStatus.ACTIVE);
    }

    @Test
    @DisplayName("댓글 수정 실패 - 댓글 없음")
    void updateComment_CommentNotFound_ThrowsException() {
        // Given
        Long commentId = 999L;
        Long userId = 1L;
        CommentUpdateRequest request = CommentUpdateRequest.builder()
                .comment("Updated Comment")
                .build();

        when(commentRepository.findByIdAndStatusWithUser(commentId, CommentStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.updateComment(commentId, request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Comment not found");
    }

    @Test
    @DisplayName("댓글 수정 실패 - 권한 없음")
    void updateComment_Unauthorized_ThrowsException() {
        // Given
        Long commentId = 1L;
        Long ownerId = 1L;
        Long requesterId = 2L;
        CommentUpdateRequest request = CommentUpdateRequest.builder()
                .comment("Updated Comment")
                .build();

        User owner = User.builder()
                .email("owner@example.com")
                .passwordHash("encoded")
                .nickname("owner")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(owner, "userId", 1L);

        Post post = Post.builder()
                .title("Test Post")
                .content("Test Content")
                .status(PostStatus.ACTIVE)
                .user(owner)
                .build();

        Comment comment = Comment.builder()
                .content("Original Comment")
                .status(CommentStatus.ACTIVE)
                .post(post)
                .user(owner)
                .build();

        when(commentRepository.findByIdAndStatusWithUser(commentId, CommentStatus.ACTIVE))
                .thenReturn(Optional.of(comment));

        // When & Then
        assertThatThrownBy(() -> commentService.updateComment(commentId, request, requesterId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Not authorized");
    }

    @Test
    @DisplayName("댓글 삭제 성공 - Soft Delete, commentCount 감소")
    void deleteComment_Success() {
        // Given
        Long commentId = 1L;
        Long userId = 1L;
        Long postId = 1L;

        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encoded")
                .nickname("testnick")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "userId", 1L);

        Post post = Post.builder()
                .title("Test Post")
                .content("Test Content")
                .status(PostStatus.ACTIVE)
                .user(user)
                .build();
        ReflectionTestUtils.setField(post, "postId", postId);

        Comment comment = Comment.builder()
                .content("Test Comment")
                .status(CommentStatus.ACTIVE)
                .post(post)
                .user(user)
                .build();

        when(commentRepository.findByIdAndStatusWithUser(commentId, CommentStatus.ACTIVE))
                .thenReturn(Optional.of(comment));
        when(postStatsRepository.decrementCommentCount(postId)).thenReturn(1);

        // When
        commentService.deleteComment(commentId, userId);

        // Then
        verify(commentRepository, times(1)).findByIdAndStatusWithUser(commentId, CommentStatus.ACTIVE);
        verify(postStatsRepository, times(1)).decrementCommentCount(anyLong());
        // Soft Delete이므로 실제 삭제 메서드는 호출되지 않음
        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 권한 없음")
    void deleteComment_Unauthorized_ThrowsException() {
        // Given
        Long commentId = 1L;
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
                .title("Test Post")
                .content("Test Content")
                .status(PostStatus.ACTIVE)
                .user(owner)
                .build();

        Comment comment = Comment.builder()
                .content("Test Comment")
                .status(CommentStatus.ACTIVE)
                .post(post)
                .user(owner)
                .build();

        when(commentRepository.findByIdAndStatusWithUser(commentId, CommentStatus.ACTIVE))
                .thenReturn(Optional.of(comment));

        // When & Then
        assertThatThrownBy(() -> commentService.deleteComment(commentId, requesterId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Not authorized");

        verify(postStatsRepository, never()).decrementCommentCount(anyLong());
    }
}
