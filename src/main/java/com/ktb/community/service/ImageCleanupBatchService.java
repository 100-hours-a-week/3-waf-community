package com.ktb.community.service;

import com.ktb.community.entity.Image;
import com.ktb.community.enums.ErrorCode;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이미지 정리 배치 서비스
 * - 고아 이미지 자동 삭제 (expires_at < NOW())
 * - 매일 새벽 3시 실행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageCleanupBatchService {

    private final ImageRepository imageRepository;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    /**
     * 고아 이미지 정리 배치 작업 (FR-IMAGE-002)
     * - 스케줄: 매일 새벽 3시 (CRON: 0 0 3 * * ?)
     * - expires_at < NOW() 조건 이미지 조회
     * - S3 파일 삭제
     * - DB 레코드 삭제 (Hard Delete)
     * - 배치 로그 기록 (성공/실패 카운트)
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOrphanImages() {
        log.info("[Batch] 고아 이미지 정리 배치 시작");
        long startTime = System.currentTimeMillis();

        LocalDateTime now = LocalDateTime.now();
        List<Image> expiredImages = imageRepository.findByExpiresAtBefore(now);

        if (expiredImages.isEmpty()) {
            log.info("[Batch] 삭제할 고아 이미지 없음");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (Image image : expiredImages) {
            try {
                // 1. S3 파일 삭제
                String s3Key = extractS3Key(image.getImageUrl());
                deleteFromS3(s3Key);

                // 2. DB 레코드 삭제 (Hard Delete)
                imageRepository.delete(image);

                successCount++;
                log.debug("[Batch] 이미지 삭제 성공: imageId={}, s3Key={}",
                         image.getImageId(), s3Key);

            } catch (Exception e) {
                failCount++;
                log.error("[Batch] 이미지 삭제 실패: imageId={}, error={}",
                         image.getImageId(), e.getMessage(), e);
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("[Batch] 고아 이미지 정리 완료: 성공={}, 실패={}, 전체={}, 소요시간={}ms",
                successCount, failCount, expiredImages.size(), elapsedTime);
    }

    /**
     * S3 파일 삭제
     */
    private void deleteFromS3(String s3Key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

        } catch (Exception e) {
            log.error("[Batch] S3 파일 삭제 실패: s3Key={}, error={}", s3Key, e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * S3 URL에서 키 추출
     * 예시: https://bucket-name.s3.region.amazonaws.com/images/2025/10/11/test.jpg
     *       → images/2025/10/11/test.jpg
     */
    private String extractS3Key(String imageUrl) {
        int keyStartIndex = imageUrl.indexOf(".com/") + 5;
        return imageUrl.substring(keyStartIndex);
    }
}
