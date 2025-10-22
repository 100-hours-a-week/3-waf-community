package com.ktb.community.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 이미지 엔티티
 * DDL: images 테이블
 */
@Entity
@Table(name = "images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Image extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    @Column(name = "file_size")
    private Integer fileSize;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Builder
    public Image(String imageUrl, Integer fileSize, String originalFilename, LocalDateTime expiresAt) {
        this.imageUrl = imageUrl;
        this.fileSize = fileSize;
        this.originalFilename = originalFilename;
        this.expiresAt = expiresAt;
    }

    /**
     * 이미지를 영구 보존하기 위해 만료 시간 제거
     * 게시글이나 프로필에 연결될 때 호출
     */
    public void clearExpiresAt() {
        this.expiresAt = null;
    }

    /**
     * 고아 이미지로 전환 (TTL 복원)
     * 게시글/프로필에서 제거될 때 호출
     *
     * @param expiresAt 만료 시간 (기본: 현재 + 1시간)
     */
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
