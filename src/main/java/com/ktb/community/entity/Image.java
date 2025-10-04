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
public class Image {

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public Image(String imageUrl, Integer fileSize, String originalFilename) {
        this.imageUrl = imageUrl;
        this.fileSize = fileSize;
        this.originalFilename = originalFilename;
    }
}
