package com.ktb.community.service;

import com.ktb.community.dto.request.PostCreateRequest;
import com.ktb.community.dto.request.PostUpdateRequest;
import com.ktb.community.dto.response.PostResponse;
import com.ktb.community.entity.Post;
import com.ktb.community.entity.PostStats;
import com.ktb.community.entity.User;
import com.ktb.community.enums.ErrorCode;
import com.ktb.community.enums.PostStatus;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.repository.PostRepository;
import com.ktb.community.repository.PostStatsRepository;
import com.ktb.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 게시글 서비스
 * FR-POST-001~005
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostStatsRepository postStatsRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    /**
     * 게시글 작성 (FR-POST-001)
     */
    @Transactional
    public PostResponse createPost(PostCreateRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "User not found with id: " + userId));

        // 게시글 생성
        Post post = request.toEntity(user);
        Post savedPost = postRepository.save(post);

        // 통계 초기화 (카운트는 Builder에서 0으로 자동 설정)
        PostStats stats = PostStats.builder()
                .post(savedPost)
                .build();
        PostStats savedStats = postStatsRepository.save(stats);

        // Post에 stats 연결 (PostResponse에서 null 방지)
        savedPost.updateStats(savedStats);

        log.info("Post created: postId={}, userId={}", savedPost.getPostId(), userId);

        return PostResponse.from(savedPost);
    }

    /**
     * 게시글 목록 조회 (FR-POST-002)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPosts(int offset, int limit, String sort) {
        // 페이지 정보 생성
        int page = offset / limit;
        Pageable pageable = PageRequest.of(page, limit, getSort(sort));

        // 게시글 조회 (Fetch Join으로 N+1 방지)
        Page<Post> postPage = postRepository.findByStatusWithUserAndStats(PostStatus.ACTIVE, pageable);

        // DTO 변환
        List<PostResponse> posts = postPage.getContent().stream()
                .map(PostResponse::from)
                .collect(Collectors.toList());

        // 응답 구성
        Map<String, Object> response = new HashMap<>();
        response.put("posts", posts);

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("total_count", postPage.getTotalElements());
        response.put("pagination", pagination);

        return response;
    }

    /**
     * 게시글 상세 조회 (FR-POST-003)
     */
    @Transactional
    public PostResponse getPostDetail(Long postId) {
        Post post = postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND,
                        "Post not found with id: " + postId));

        // 조회수 증가 (동시성 제어)
        postStatsRepository.incrementViewCount(postId);

        // 영속성 컨텍스트에서 stats를 DB 상태로 동기화
        if (post.getStats() != null) {
            entityManager.refresh(post.getStats());
        }

        log.info("Post viewed: postId={}", postId);

        return PostResponse.from(post);
    }

    /**
     * 게시글 수정 (FR-POST-004)
     */
    @Transactional
    public PostResponse updatePost(Long postId, PostUpdateRequest request, Long userId) {
        // 최소 1개 필드 검증
        if (!request.hasAnyUpdate()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "At least one field must be provided for update");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND,
                        "Post not found with id: " + postId));

        // 권한 검증
        if (!post.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS,
                    "Not authorized to update this post");
        }

        // 부분 업데이트
        if (request.getTitle() != null) {
            post.updateTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            post.updateContent(request.getContent());
        }

        log.info("Post updated: postId={}, userId={}", postId, userId);

        return PostResponse.from(post);
    }

    /**
     * 게시글 삭제 (FR-POST-005)
     */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND,
                        "Post not found with id: " + postId));

        // 권한 검증
        if (!post.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS,
                    "Not authorized to delete this post");
        }

        // Soft Delete
        post.updateStatus(PostStatus.DELETED);

        log.info("Post deleted: postId={}, userId={}", postId, userId);
    }

    /**
     * 정렬 조건 생성
     */
    private Sort getSort(String sort) {
        if ("likes".equalsIgnoreCase(sort)) {
            return Sort.by(
                    Sort.Order.desc("stats.likeCount"),
                    Sort.Order.desc("createdAt")
            );
        }
        // 기본: latest
        return Sort.by(Sort.Order.desc("createdAt"));
    }
}
