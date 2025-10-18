package com.ktb.community.service;

import com.ktb.community.dto.request.PostCreateRequest;
import com.ktb.community.dto.request.PostUpdateRequest;
import com.ktb.community.dto.response.PostResponse;
import com.ktb.community.entity.Post;
import com.ktb.community.entity.PostStats;
import com.ktb.community.entity.User;
import com.ktb.community.enums.ErrorCode;
import com.ktb.community.enums.PostStatus;
import com.ktb.community.enums.UserStatus;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.entity.Image;
import com.ktb.community.entity.PostImage;
import com.ktb.community.repository.ImageRepository;
import com.ktb.community.repository.PostImageRepository;
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
    private final ImageRepository imageRepository;
    private final PostImageRepository postImageRepository;
    private final EntityManager entityManager;

    /**
     * 게시글 작성 (FR-POST-001)
     * - 사용자 존재 확인
     * - 게시글 생성 및 통계 초기화
     * - 이미지 연결 (imageId 있을 경우)
     * - 이미지 expires_at 클리어 (영구 보존)
     */
    @Transactional
    public PostResponse createPost(PostCreateRequest request, Long userId) {
        User user = userRepository.findByUserIdAndUserStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "User not found or inactive with id: " + userId));

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

        // 이미지 연결 처리 (imageId가 있을 경우)
        if (request.getImageId() != null) {
            Image image = imageRepository.findById(request.getImageId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND,
                            "Image not found with id: " + request.getImageId()));

            // expires_at 클리어 (영구 보존)
            image.clearExpiresAt();

            // PostImage 브릿지 테이블 저장
            PostImage postImage = PostImage.builder()
                    .post(savedPost)
                    .image(image)
                    .displayOrder(1)
                    .build();
            postImageRepository.save(postImage);

            log.info("[Post] 게시글 이미지 연결: postId={}, imageId={}", savedPost.getPostId(), image.getImageId());
        }

        log.debug("[Post] 게시글 작성 완료: postId={}", savedPost.getPostId());

        return PostResponse.from(savedPost);
    }

    /**
     * 게시글 목록 조회 (FR-POST-002)
     * - ACTIVE 상태만 조회
     * - Fetch Join (N+1 방지)
     * - 정렬: latest (cursor) / likes (offset)
     * - 하이브리드 페이지네이션
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPosts(Long cursor, Integer offset, int limit, String sort) {
        if ("latest".equalsIgnoreCase(sort)) {
            return getPostsCursor(cursor, limit);
        } else {
            return getPostsOffset(offset != null ? offset : 0, limit, sort);
        }
    }

    /**
     * Cursor 기반 게시글 목록 조회 (latest 전용)
     */
    private Map<String, Object> getPostsCursor(Long cursor, int limit) {
        // limit+1 조회 (hasMore 판단용)
        List<Post> posts = (cursor == null)
                ? postRepository.findByStatusWithoutCursor(PostStatus.ACTIVE, PageRequest.of(0, limit + 1))
                : postRepository.findByStatusWithCursor(PostStatus.ACTIVE, cursor, PageRequest.of(0, limit + 1));

        // hasMore 판단 및 초과 데이터 제거
        boolean hasMore = posts.size() > limit;
        if (hasMore) {
            posts.remove(limit);
        }

        // nextCursor 계산
        Long nextCursor = hasMore && !posts.isEmpty()
                ? posts.get(posts.size() - 1).getPostId()
                : null;

        // DTO 변환
        List<PostResponse> postResponses = posts.stream()
                .map(PostResponse::from)
                .collect(Collectors.toList());

        // 응답 구성 (cursor 방식)
        Map<String, Object> response = new HashMap<>();
        response.put("posts", postResponses);
        response.put("nextCursor", nextCursor);
        response.put("hasMore", hasMore);

        log.debug("[Post] Cursor 게시글 목록 조회 완료: cursor={}, count={}, hasMore={}", cursor, posts.size(), hasMore);

        return response;
    }

    /**
     * Offset 기반 게시글 목록 조회 (likes 등)
     */
    private Map<String, Object> getPostsOffset(int offset, int limit, String sort) {
        // 페이지 정보 생성
        int page = offset / limit;
        Pageable pageable = PageRequest.of(page, limit, getSort(sort));

        // 게시글 조회 (Fetch Join으로 N+1 방지)
        Page<Post> postPage = postRepository.findByStatusWithUserAndStats(PostStatus.ACTIVE, pageable);

        // DTO 변환
        List<PostResponse> posts = postPage.getContent().stream()
                .map(PostResponse::from)
                .collect(Collectors.toList());

        // 응답 구성 (offset 방식)
        Map<String, Object> response = new HashMap<>();
        response.put("posts", posts);

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("total_count", postPage.getTotalElements());
        response.put("pagination", pagination);

        log.debug("[Post] Offset 게시글 목록 조회 완료: offset={}, count={}, total={}", offset, posts.size(), postPage.getTotalElements());

        return response;
    }

    /**
     * 게시글 상세 조회 (FR-POST-003)
     * - ACTIVE 상태만 조회
     * - Fetch Join (N+1 방지)
     * - 조회수 자동 증가 (동시성 제어)
     */
    @Transactional
    public PostResponse getPostDetail(Long postId) {
        Post post = postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND,
                        "Post not found with id: " + postId));

        // 조회수 증가 (동시성 제어)
        postStatsRepository.incrementViewCount(postId);

        // Optimistic Update: 클라이언트가 UI에서 +1 처리 (detail.js)
        return PostResponse.from(post);
    }

    /**
     * 게시글 수정 (FR-POST-004)
     * - 작성자 본인만 수정 가능
     * - 최소 1개 필드 필요 (부분 업데이트)
     * - 이미지 변경 시 기존 연결 해제 후 재연결
     */
    @Transactional
    public PostResponse updatePost(Long postId, PostUpdateRequest request, Long userId) {
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

        // 이미지 업데이트 (imageId가 있을 경우)
        if (request.getImageId() != null) {
            // 기존 이미지 연결 삭제
            postImageRepository.deleteByPostId(postId);

            // 새 이미지 연결
            Image image = imageRepository.findById(request.getImageId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND,
                            "Image not found with id: " + request.getImageId()));

            // expires_at 클리어 (영구 보존)
            image.clearExpiresAt();

            // PostImage 브릿지 테이블 저장
            PostImage postImage = PostImage.builder()
                    .post(post)
                    .image(image)
                    .displayOrder(1)
                    .build();
            postImageRepository.save(postImage);

            log.info("[Post] 게시글 이미지 변경: postId={}, imageId={}", postId, image.getImageId());
        }

        log.debug("[Post] 게시글 수정 완료: postId={}", postId);

        return PostResponse.from(post);
    }

    /**
     * 게시글 삭제 (FR-POST-005)
     * - 작성자 본인만 삭제 가능
     * - Soft Delete (상태 → DELETED)
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

        log.debug("[Post] 게시글 삭제 완료: postId={}", postId);
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
