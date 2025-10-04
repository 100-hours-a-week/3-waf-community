package com.ktb.community.entity;

import com.ktb.community.enums.UserRole;
import com.ktb.community.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 엔티티
 * DDL: users 테이블
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"posts", "comments", "postLikes", "tokens", "profileImage"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, unique = true, length = 30)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('USER', 'ADMIN') DEFAULT 'USER'")
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false, columnDefinition = "ENUM('ACTIVE', 'INACTIVE', 'DELETED') DEFAULT 'ACTIVE'")
    private UserStatus userStatus = UserStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 프로필 이미지 (1:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private Image profileImage;

    // 양방향 관계 (LLD.md 기준)
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<PostLike> postLikes = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<UserToken> tokens = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public User(String email, String passwordHash, String nickname, UserRole role, Image profileImage) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.role = role != null ? role : UserRole.USER;
        this.userStatus = UserStatus.ACTIVE;
        this.profileImage = profileImage;
    }
}
