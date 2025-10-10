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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 게시글 서비스
 * PRD.md FR-POST-001~005 참조
 * LLD.md Section 7.1 참조
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostStatsRepository postStatsRepository;
    private final UserRepository userRepository;
    // Phase 3.5+: ImageRepository, PostImageRepository 추가 예정

    /**
     * 게시글 작성 (FR-POST-001)
     * - Post 엔티티 생성 및 저장
     * - PostStats 초기화 (카운트 0)
     * - Phase 3.5+: 이미지 연결 처리
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
        postStatsRepository.save(stats);

        // Phase 3.5+: 이미지 연결 처리
        if (request.getImageId() != null) {
            // TODO: Phase 3.5+ ImageRepository, PostImageRepository 추가 후 구현
            // Image image = imageRepository.findById(request.getImageId())
            //     .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
            // image.clearExpiresAt();  // 영구 보존
            // PostImage postImage = PostImage.builder()
            //     .post(savedPost).image(image).displayOrder(1).build();
            // postImageRepository.save(postImage);
            log.info("Image connection requested for post (Phase 3.5+): imageId={}", request.getImageId());
        }

        log.info("Post created: postId={}, userId={}", savedPost.getPostId(), userId);

        return PostResponse.from(savedPost);
    }

    /**
     * 게시글 목록 조회 (FR-POST-002)
     * - Offset/Limit 페이지네이션
     * - 정렬: latest(최신순), likes(좋아요순)
     * - ACTIVE만 조회
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
     * - ACTIVE만 조회
     * - 조회수 자동 증가 (원자적 UPDATE)
     */
    @Transactional
    public PostResponse getPostDetail(Long postId) {
        Post post = postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND,
                        "Post not found with id: " + postId));

        // 조회수 증가 (LLD.md Section 12.3 동시성 제어)
        postStatsRepository.incrementViewCount(postId);

        log.info("Post viewed: postId={}", postId);

        return PostResponse.from(post);
    }

    /**
     * 게시글 수정 (FR-POST-004)
     * - 작성자 본인만 수정 가능
     * - PATCH: 부분 업데이트 (최소 1개 필드 필요)
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

        // Phase 3.5+: 이미지 업데이트 처리
        if (request.getImageId() != null) {
            // TODO: Phase 3.5+ 이미지 연결 업데이트
            // postImageRepository.deleteByPostPostId(postId);  // 기존 연결 삭제
            // Image image = imageRepository.findById(request.getImageId())
            //     .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
            // image.clearExpiresAt();
            // PostImage postImage = PostImage.builder()
            //     .post(post).image(image).displayOrder(1).build();
            // postImageRepository.save(postImage);
            log.info("Image update requested for post (Phase 3.5+): postId={}, imageId={}",
                    postId, request.getImageId());
        }

        log.info("Post updated: postId={}, userId={}", postId, userId);

        return PostResponse.from(post);
    }

    /**
     * 게시글 삭제 (FR-POST-005)
     * - 작성자 본인만 삭제 가능
     * - Soft Delete (status → DELETED)
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
     * - latest: 최신순 (created_at DESC)
     * - likes: 좋아요순 (like_count DESC, created_at DESC)
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
