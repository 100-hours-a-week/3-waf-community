package com.ktb.community.service;

import com.ktb.community.dto.request.CommentCreateRequest;
import com.ktb.community.dto.request.CommentUpdateRequest;
import com.ktb.community.dto.response.CommentResponse;
import com.ktb.community.entity.Comment;
import com.ktb.community.entity.Post;
import com.ktb.community.entity.User;
import com.ktb.community.enums.CommentStatus;
import com.ktb.community.enums.ErrorCode;
import com.ktb.community.enums.PostStatus;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.repository.CommentRepository;
import com.ktb.community.repository.PostRepository;
import com.ktb.community.repository.PostStatsRepository;
import com.ktb.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 댓글 서비스
 * PRD.md FR-COMMENT-001~004 참조
 * LLD.md Section 7.4 참조
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final PostStatsRepository postStatsRepository;
    private final UserRepository userRepository;

    /**
     * 댓글 작성 (FR-COMMENT-001)
     * - 게시글 존재 확인 (ACTIVE만)
     * - Comment 저장
     * - PostStats.commentCount 원자적 증가
     */
    @Transactional
    public CommentResponse createComment(Long postId, CommentCreateRequest request, Long userId) {
        // 게시글 존재 확인
        Post post = postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND,
                        "Post not found with id: " + postId));

        // 사용자 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "User not found with id: " + userId));

        // 댓글 생성
        Comment comment = request.toEntity(post, user);
        Comment savedComment = commentRepository.save(comment);

        // 댓글 수 자동 증가 (LLD.md Section 12.3 동시성 제어)
        postStatsRepository.incrementCommentCount(postId);

        log.info("Comment created: commentId={}, postId={}, userId={}",
                savedComment.getCommentId(), postId, userId);

        return CommentResponse.from(savedComment);
    }

    /**
     * 댓글 목록 조회 (FR-COMMENT-002)
     * - 특정 게시글의 댓글만 조회 (ACTIVE)
     * - 생성일 오름차순
     * - Offset/Limit 페이지네이션
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getComments(Long postId, int offset, int limit) {
        // 게시글 존재 확인
        if (!postRepository.existsByPostIdAndStatus(postId, PostStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND,
                    "Post not found with id: " + postId);
        }

        // 페이지 정보 생성
        int page = offset / limit;
        Pageable pageable = PageRequest.of(page, limit);

        // 댓글 조회 (Fetch Join으로 N+1 방지)
        Page<Comment> commentPage = commentRepository.findByPostIdAndStatusWithUser(
                postId, CommentStatus.ACTIVE, pageable);

        // DTO 변환
        List<CommentResponse> comments = commentPage.getContent().stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());

        // 응답 구성
        Map<String, Object> response = new HashMap<>();
        response.put("comments", comments);

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("total_count", commentPage.getTotalElements());
        response.put("pagination", pagination);

        return response;
    }

    /**
     * 댓글 수정 (FR-COMMENT-003)
     * - 작성자 본인만 수정 가능
     * - 내용만 수정
     */
    @Transactional
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request, Long userId) {
        Comment comment = commentRepository.findByIdAndStatusWithUser(commentId, CommentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND,
                        "Comment not found with id: " + commentId));

        // 권한 검증
        if (!comment.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS,
                    "Not authorized to update this comment");
        }

        // 내용 수정
        comment.updateContent(request.getComment());

        log.info("Comment updated: commentId={}, userId={}", commentId, userId);

        return CommentResponse.from(comment);
    }

    /**
     * 댓글 삭제 (FR-COMMENT-004)
     * - 작성자 본인만 삭제 가능
     * - Soft Delete (status → DELETED)
     * - PostStats.commentCount 원자적 감소
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findByIdAndStatusWithUser(commentId, CommentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND,
                        "Comment not found with id: " + commentId));

        // 권한 검증
        if (!comment.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS,
                    "Not authorized to delete this comment");
        }

        // Soft Delete
        comment.updateStatus(CommentStatus.DELETED);

        // 댓글 수 자동 감소 (LLD.md Section 12.3)
        postStatsRepository.decrementCommentCount(comment.getPost().getPostId());

        log.info("Comment deleted: commentId={}, userId={}", commentId, userId);
    }
}
