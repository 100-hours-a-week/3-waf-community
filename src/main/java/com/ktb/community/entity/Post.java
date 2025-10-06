package com.ktb.community.entity;

import com.ktb.community.enums.PostStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 게시글 엔티티
 * DDL: posts 테이블
 */
@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"user", "comments", "postLikes", "stats", "postImages"})
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "post_title", nullable = false, length = 100)
    private String postTitle;

    @Column(name = "post_content", nullable = false, columnDefinition = "LONGTEXT")
    private String postContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_status", nullable = false, columnDefinition = "ENUM('ACTIVE', 'DELETED', 'DRAFT') DEFAULT 'ACTIVE'")
    private PostStatus postStatus = PostStatus.ACTIVE;

    // 작성자 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 양방향 관계 (LLD.md 기준)
    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
    private List<PostLike> postLikes = new ArrayList<>();

    @OneToOne(mappedBy = "post", fetch = FetchType.LAZY)
    private PostStats stats;

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
    private List<PostImage> postImages = new ArrayList<>();

    @Builder
    public Post(String postTitle, String postContent, User user) {
        this.postTitle = postTitle;
        this.postContent = postContent;
        this.user = user;
        this.postStatus = PostStatus.ACTIVE;
    }
}
