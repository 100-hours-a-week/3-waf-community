package com.ktb.community.service;

import com.ktb.community.entity.Image;
import com.ktb.community.enums.ErrorCode;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
public class ImageCleanupBatchService {

    private final ImageRepository imageRepository;
    private final S3Client s3Client;
    private final ImageCleanupBatchService self;  // Self-injection for proxy access

    @Value("${aws.s3.bucket}")
    private String bucketName;

    /**
     * Self-injection 생성자
     * @param self @Lazy로 주입하여 순환 의존성 방지
     */
    public ImageCleanupBatchService(
            ImageRepository imageRepository,
            S3Client s3Client,
            @Lazy ImageCleanupBatchService self
    ) {
        this.imageRepository = imageRepository;
        this.s3Client = s3Client;
        this.self = self;
    }

    /**
     * 고아 이미지 정리 배치 작업 (FR-IMAGE-002)
     * - 스케줄: 매일 새벽 3시 (CRON: 0 0 3 * * ?)
     * - expires_at < NOW() 조건 이미지 조회
     * - S3 파일 삭제
     * - DB 레코드 삭제 (Hard Delete)
     * - 배치 로그 기록 (성공/실패 카운트)
     * 
     * 트랜잭션 전략:
     * - 배치 전체는 비트랜잭션 (조회만 수행)
     * - 각 이미지 삭제는 self.deleteImageInNewTransaction()을 통해 프록시 호출
     * - 프록시를 통한 호출로 REQUIRES_NEW 트랜잭션이 실제로 적용됨
     * - 개별 실패가 전체 배치에 영향을 주지 않음
     * 
     * Spring AOP 프록시 우회 방지:
     * - Self-injection 패턴으로 자신의 프록시를 주입받음
     * - 내부 메서드 호출 시 this 대신 self 사용
     * - @Lazy로 순환 의존성 방지
     */
    @Scheduled(cron = "0 0 3 * * ?")
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
                // 프록시를 통해 호출하여 REQUIRES_NEW 트랜잭션 적용
                self.deleteImageInNewTransaction(image);
                successCount++;
                log.debug("[Batch] 이미지 삭제 성공: imageId={}", image.getImageId());

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
     * 독립적인 트랜잭션으로 이미지 삭제
     * - REQUIRES_NEW: 호출자의 트랜잭션과 무관하게 새 트랜잭션 생성
     * - 이미지 삭제 실패 시 해당 트랜잭션만 롤백
     * - 다른 이미지 삭제에 영향을 주지 않음
     * 
     * @param image 삭제할 이미지 엔티티
     * @throws Exception S3 삭제 또는 DB 삭제 실패 시
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteImageInNewTransaction(Image image) {
        // 1. S3 파일 삭제
        String s3Key = extractS3Key(image.getImageUrl());
        deleteFromS3(s3Key);

        // 2. DB 레코드 삭제 (Hard Delete)
        imageRepository.delete(image);
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
