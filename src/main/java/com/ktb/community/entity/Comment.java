package com.ktb.community.entity;

import com.ktb.community.enums.CommentStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * 댓글 엔티티
 * DDL: comments 테이블
 */
@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"post", "user"})
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "comment_content", nullable = false, length = 600)
    private String commentContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_status", nullable = false, columnDefinition = "ENUM('ACTIVE', 'DELETED') DEFAULT 'ACTIVE'")
    private CommentStatus commentStatus = CommentStatus.ACTIVE;

    // 게시글 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // 작성자 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public Comment(String content, CommentStatus status, Post post, User user) {
        this.commentContent = content;
        this.commentStatus = status != null ? status : CommentStatus.ACTIVE;
        this.post = post;
        this.user = user;
    }

    /**
     * 내용 수정
     */
    public void updateContent(String content) {
        this.commentContent = content;
    }

    /**
     * 상태 변경
     */
    public void updateStatus(CommentStatus status) {
        this.commentStatus = status;
    }

    /**
     * Getter 별칭 (CommentResponse에서 사용)
     */
    public String getContent() {
        return this.commentContent;
    }

    public CommentStatus getStatus() {
        return this.commentStatus;
    }
}
