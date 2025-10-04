package com.ktb.community.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자 토큰 엔티티
 * DDL: user_tokens 테이블
 */
@Entity
@Table(name = "user_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"user"})
public class UserToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_token_id")
    private Long userTokenId;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // 사용자 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public UserToken(String token, LocalDateTime expiresAt, String userAgent, String ipAddress, User user) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.user = user;
    }
}
