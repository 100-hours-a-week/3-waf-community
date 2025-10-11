package com.ktb.community.dto.response;

import com.ktb.community.entity.Image;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 이미지 응답 DTO
 * API.md Section 4.1 참조
 */
@Getter
@Builder
public class ImageResponse {

    private Long imageId;
    private String imageUrl;
    private Integer fileSize;
    private String originalFilename;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    /**
     * Entity → DTO 변환
     */
    public static ImageResponse from(Image image) {
        return ImageResponse.builder()
                .imageId(image.getImageId())
                .imageUrl(image.getImageUrl())
                .fileSize(image.getFileSize())
                .originalFilename(image.getOriginalFilename())
                .createdAt(image.getCreatedAt())
                .expiresAt(image.getExpiresAt())
                .build();
    }
}
