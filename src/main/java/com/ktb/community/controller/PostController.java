package com.ktb.community.controller;

import com.ktb.community.config.RateLimit;
import com.ktb.community.dto.ApiResponse;
import com.ktb.community.dto.request.PostCreateRequest;
import com.ktb.community.dto.request.PostUpdateRequest;
import com.ktb.community.dto.response.PostResponse;
import com.ktb.community.service.LikeService;
import com.ktb.community.service.PostService;
import com.ktb.community.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// [세션 전환] JWT 방식 (미사용)
// import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 게시글 컨트롤러
 * API.md Section 3, 6 참조
 */
@Slf4j
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final LikeService likeService;
    private final UserService userService;

    /**
     * 게시글 목록 조회 (API.md Section 3.1)
     * - latest: GET /posts?cursor=123&limit=10&sort=latest
     * - likes: GET /posts?offset=0&limit=10&sort=likes
     * Tier 3: 제한 없음 (조회 API, 페이지네이션 있음)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPosts(
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer offset,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        Map<String, Object> result = postService.getPosts(cursor, offset, limit, sort);
        return ResponseEntity.ok(ApiResponse.success("get_posts_success", result));
    }

    /**
     * 게시글 상세 조회 (API.md Section 3.2)
     * GET /posts/{postId}
     * Tier 3: 제한 없음 (조회 API)
     */
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostDetail(
            @PathVariable Long postId,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");  // null for non-logged-in users
        PostResponse post = postService.getPostDetail(postId, userId);
        return ResponseEntity.ok(ApiResponse.success("get_post_detail_success", post));
    }

    /**
     * 게시글 작성 (API.md Section 3.3)
     * POST /posts
     * Authorization: Bearer {access_token}
     * Tier 2: 중간 제한 (게시글 spam 방지)
     */
    @PostMapping
    @RateLimit(requestsPerMinute = 30)
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @Valid @RequestBody PostCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        PostResponse post = postService.createPost(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("create_post_success", post));
    }

    /**
     * 게시글 수정 (API.md Section 3.4)
     * PATCH /posts/{postId}
     * Authorization: Bearer {access_token}
     * Tier 3: 넉넉한 제한 (본인 권한 검증 있음)
     */
    @PatchMapping("/{postId}")
    @RateLimit(requestsPerMinute = 50)
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        PostResponse post = postService.updatePost(postId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("update_post_success", post));
    }

    /**
     * 게시글 삭제 (API.md Section 3.5)
     * DELETE /posts/{postId}
     * Authorization: Bearer {access_token}
     * Tier 3: 넉넉한 제한 (Soft Delete, 본인 권한 검증)
     */
    @DeleteMapping("/{postId}")
    @RateLimit(requestsPerMinute = 30)
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        postService.deletePost(postId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 게시글 좋아요 (API.md Section 6.1)
     * POST /posts/{postId}/like
     * Authorization: Bearer {access_token}
     * Tier 3: 넉넉한 제한 (빈번한 액션, 원자적 UPDATE로 안전)
     */
    @PostMapping("/{postId}/like")
    @RateLimit(requestsPerMinute = 200)
    public ResponseEntity<ApiResponse<Map<String, String>>> addLike(
            @PathVariable Long postId,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Map<String, String> result = likeService.addLike(postId, userId);
        return ResponseEntity.ok(ApiResponse.success("like_success", result));
    }

    /**
     * 게시글 좋아요 취소 (API.md Section 6.2)
     * DELETE /posts/{postId}/like
     * Authorization: Bearer {access_token}
     * Tier 3: 넉넉한 제한 (빈번한 액션, 원자적 UPDATE로 안전)
     */
    @DeleteMapping("/{postId}/like")
    @RateLimit(requestsPerMinute = 200)
    public ResponseEntity<ApiResponse<Map<String, String>>> removeLike(
            @PathVariable Long postId,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Map<String, String> result = likeService.removeLike(postId, userId);
        return ResponseEntity.ok(ApiResponse.success("unlike_success", result));
    }

    /**
     * 내가 좋아요한 게시글 목록 조회 (API.md Section 6.3)
     * GET /users/me/likes?offset=0&limit=10
     * Authorization: Bearer {access_token}
     * Tier 3: 제한 없음 (조회 API)
     */
    @GetMapping("/users/me/likes")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLikedPosts(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Map<String, Object> result = likeService.getLikedPosts(userId, offset, limit);
        return ResponseEntity.ok(ApiResponse.success("get_liked_posts_success", result));
    }

    // [세션 전환] JWT 방식 getUserId (미사용)
    // /**
    //  * 인증된 사용자 ID 추출
    //  * JWT subject에서 userId 직접 파싱 (성능 최적화)
    //  */
    // private Long getUserId(Authentication authentication) { ... }
}
