package com.ktb.community.repository;

import com.ktb.community.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 이미지 Repository
 * FR-IMAGE-001, FR-IMAGE-003
 */
@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
}
