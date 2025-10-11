package com.ktb.community.repository;

import com.ktb.community.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이미지 Repository
 * FR-IMAGE-001, FR-IMAGE-003, FR-IMAGE-002 (고아 이미지)
 */
@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    /**
     * 만료된 이미지 조회 (고아 이미지 배치용)
     * - expires_at < 지정 시간
     * - idx_images_expires 인덱스 활용
     */
    List<Image> findByExpiresAtBefore(LocalDateTime dateTime);
}
