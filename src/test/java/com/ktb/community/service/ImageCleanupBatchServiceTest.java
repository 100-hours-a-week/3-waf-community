package com.ktb.community.service;

import com.ktb.community.entity.Image;
import com.ktb.community.repository.ImageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ImageCleanupBatchService 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("고아 이미지 배치 테스트")
class ImageCleanupBatchServiceTest {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private ImageCleanupBatchService batchService;

    @Test
    @DisplayName("고아 이미지 배치 - 만료된 이미지 삭제 성공")
    void cleanupOrphanImages_Success() {
        // Given
        Image expiredImage = Image.builder()
                .imageUrl("https://test-bucket.s3.ap-northeast-2.amazonaws.com/images/2025/10/11/test.jpg")
                .fileSize(1024)
                .originalFilename("test.jpg")
                .expiresAt(LocalDateTime.now().minusHours(2))
                .build();

        when(imageRepository.findByExpiresAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(expiredImage));

        // When
        batchService.cleanupOrphanImages();

        // Then
        verify(imageRepository).findByExpiresAtBefore(any(LocalDateTime.class));
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        verify(imageRepository).delete(expiredImage);
    }

    @Test
    @DisplayName("고아 이미지 배치 - 만료된 이미지 없음")
    void cleanupOrphanImages_NoExpiredImages() {
        // Given
        when(imageRepository.findByExpiresAtBefore(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        batchService.cleanupOrphanImages();

        // Then
        verify(imageRepository).findByExpiresAtBefore(any(LocalDateTime.class));
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        verify(imageRepository, never()).delete(any(Image.class));
    }

    @Test
    @DisplayName("고아 이미지 배치 - 여러 이미지 동시 삭제")
    void cleanupOrphanImages_MultipleImages() {
        // Given
        Image image1 = Image.builder()
                .imageUrl("https://test-bucket.s3.ap-northeast-2.amazonaws.com/images/test1.jpg")
                .expiresAt(LocalDateTime.now().minusHours(2))
                .build();

        Image image2 = Image.builder()
                .imageUrl("https://test-bucket.s3.ap-northeast-2.amazonaws.com/images/test2.jpg")
                .expiresAt(LocalDateTime.now().minusHours(3))
                .build();

        Image image3 = Image.builder()
                .imageUrl("https://test-bucket.s3.ap-northeast-2.amazonaws.com/images/test3.jpg")
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(imageRepository.findByExpiresAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(image1, image2, image3));

        // When
        batchService.cleanupOrphanImages();

        // Then
        verify(imageRepository).findByExpiresAtBefore(any(LocalDateTime.class));
        verify(s3Client, times(3)).deleteObject(any(DeleteObjectRequest.class));
        verify(imageRepository, times(3)).delete(any(Image.class));
    }
}
