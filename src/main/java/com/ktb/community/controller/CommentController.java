package com.ktb.community.controller;

import com.ktb.community.config.RateLimit;
import com.ktb.community.dto.ApiResponse;
import com.ktb.community.dto.request.CommentCreateRequest;
import com.ktb.community.dto.request.CommentUpdateRequest;
import com.ktb.community.dto.response.CommentResponse;
import com.ktb.community.service.CommentService;
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
 * 댓글 컨트롤러
 * API.md Section 5 참조
 */
@Slf4j
@RestController
@RequestMapping("/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;

    /**
     * 댓글 목록 조회 (API.md Section 5.1)
     * GET /posts/{postId}/comments?offset=0&limit=10
     * Tier 3: 제한 없음 (조회 API, 페이지네이션 있음)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Map<String, Object> result = commentService.getComments(postId, offset, limit);
        return ResponseEntity.ok(ApiResponse.success("get_comments_success", result));
    }

    /**
     * 댓글 작성 (API.md Section 5.2)
     * POST /posts/{postId}/comments
     * Authorization: Bearer {access_token}
     * Tier 2: 중간 제한 (댓글 spam 방지, 게시글보다 빈번)
     */
    @PostMapping
    @RateLimit(requestsPerMinute = 50)
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        CommentResponse comment = commentService.createComment(postId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("create_comment_success", comment));
    }

    /**
     * 댓글 수정 (API.md Section 5.3)
     * PATCH /posts/{postId}/comments/{commentId}
     * Authorization: Bearer {access_token}
     * Tier 3: 넉넉한 제한 (본인 권한 검증 있음)
     */
    @PatchMapping("/{commentId}")
    @RateLimit(requestsPerMinute = 50)
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        CommentResponse comment = commentService.updateComment(commentId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("update_comment_success", comment));
    }

    /**
     * 댓글 삭제 (API.md Section 5.4)
     * DELETE /posts/{postId}/comments/{commentId}
     * Authorization: Bearer {access_token}
     * Note: Soft Delete (status → DELETED)
     * Tier 3: 넉넉한 제한 (Soft Delete, 본인 권한 검증)
     */
    @DeleteMapping("/{commentId}")
    @RateLimit(requestsPerMinute = 30)
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    // [세션 전환] JWT 방식 getUserId (미사용)
    // /**
    //  * 인증된 사용자 ID 추출
    //  * JWT subject에서 userId 직접 파싱 (성능 최적화)
    //  */
    // private Long getUserId(Authentication authentication) { ... }
}
