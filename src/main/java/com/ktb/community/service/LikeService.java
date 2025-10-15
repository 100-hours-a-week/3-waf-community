package com.ktb.community.service;

import com.ktb.community.dto.response.PostResponse;
import com.ktb.community.entity.Post;
import com.ktb.community.entity.PostLike;
import com.ktb.community.entity.User;
import com.ktb.community.enums.ErrorCode;
import com.ktb.community.enums.PostStatus;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.repository.PostLikeRepository;
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
 * 좋아요 서비스
 * FR-LIKE-001~003
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final PostStatsRepository postStatsRepository;
    private final UserRepository userRepository;

    /**
     * 게시글 좋아요 추가 (FR-LIKE-001)
     * - 게시글 존재 확인 (ACTIVE만)
     * - 중복 방지 (UNIQUE KEY)
     * - 좋아요 수 자동 증가 (동시성 제어)
     */
    @Transactional
    public Map<String, Integer> addLike(Long postId, Long userId) {
        // 게시글 존재 확인 (ACTIVE만)
        Post post = postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND,
                        "Post not found with id: " + postId));

        // 사용자 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "User not found with id: " + userId));

        // 중복 확인
        if (postLikeRepository.existsByUserUserIdAndPostPostId(userId, postId)) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED,
                    "User already liked this post: userId=" + userId + ", postId=" + postId);
        }

        // 좋아요 생성
        PostLike postLike = PostLike.builder()
                .user(user)
                .post(post)
                .build();
        postLikeRepository.save(postLike);

        // 좋아요 수 자동 증가 (동시성 제어)
        postStatsRepository.incrementLikeCount(postId);

        // 현재 좋아요 수 조회
        int likeCount = postStatsRepository.findById(postId)
                .map(stats -> stats.getLikeCount())
                .orElse(0);

        log.debug("[Like] 좋아요 추가 완료: postId={}, likeCount={}", postId, likeCount);

        Map<String, Integer> response = new HashMap<>();
        response.put("like_count", likeCount);
        return response;
    }

    /**
     * 게시글 좋아요 취소 (FR-LIKE-002)
     * - 좋아요 존재 확인
     * - Hard Delete (영구 삭제)
     * - 좋아요 수 자동 감소 (동시성 제어)
     */
    @Transactional
    public Map<String, Integer> removeLike(Long postId, Long userId) {
        // 게시글 존재 확인
        if (!postRepository.existsByPostIdAndPostStatus(postId, PostStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND,
                    "Post not found with id: " + postId);
        }

        // 좋아요 조회
        PostLike postLike = postLikeRepository.findByUserUserIdAndPostPostId(userId, postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIKE_NOT_FOUND,
                        "Like not found: userId=" + userId + ", postId=" + postId));

        // 좋아요 삭제 (Hard Delete)
        postLikeRepository.delete(postLike);

        // 좋아요 수 자동 감소 (동시성 제어)
        postStatsRepository.decrementLikeCount(postId);

        // 현재 좋아요 수 조회
        int likeCount = postStatsRepository.findById(postId)
                .map(stats -> stats.getLikeCount())
                .orElse(0);

        log.debug("[Like] 좋아요 취소 완료: postId={}, likeCount={}", postId, likeCount);

        Map<String, Integer> response = new HashMap<>();
        response.put("like_count", likeCount);
        return response;
    }

    /**
     * 좋아요한 게시글 목록 조회 (FR-LIKE-003)
     * - 사용자 존재 확인
     * - Fetch Join (N+1 방지)
     * - ACTIVE 게시글만 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getLikedPosts(Long userId, int offset, int limit) {
        // 사용자 확인
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND,
                    "User not found with id: " + userId);
        }

        // 페이지 정보 생성
        int page = offset / limit;
        Pageable pageable = PageRequest.of(page, limit);

        // 좋아요한 게시글 조회 (Fetch Join으로 N+1 방지)
        Page<PostLike> likePage = postLikeRepository.findByUserIdWithPost(
                userId, PostStatus.ACTIVE, pageable);

        // DTO 변환 (PostLike → Post → PostResponse)
        List<PostResponse> posts = likePage.getContent().stream()
                .map(PostLike::getPost)
                .map(PostResponse::from)
                .collect(Collectors.toList());

        // 응답 구성
        Map<String, Object> response = new HashMap<>();
        response.put("posts", posts);

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("total_count", likePage.getTotalElements());
        response.put("pagination", pagination);

        return response;
    }
}
