package com.ktb.community.service;

import com.ktb.community.dto.response.ImageResponse;
import com.ktb.community.entity.Image;
import com.ktb.community.enums.ErrorCode;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.repository.ImageRepository;
import com.ktb.community.util.FileValidator;
import com.ktb.community.util.S3KeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 이미지 업로드 서비스
 * LLD.md Section 7.5 참조 (S3 직접 연동 방식)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private final S3Client s3Client;
    private final ImageRepository imageRepository;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * 이미지 업로드
     * 1. 파일 검증 (MIME type, Magic Number)
     * 2. S3 업로드
     * 3. DB 저장 (expires_at = 1시간 후)
     *
     * @param file 업로드할 이미지 파일
     * @return 이미지 정보
     */
    @Transactional
    public ImageResponse uploadImage(MultipartFile file) {
        // 1. 파일 검증
        FileValidator.validateImageFile(file);

        // 2. S3 업로드
        String s3Key = S3KeyGenerator.generateKey(file.getOriginalFilename());
        String imageUrl = uploadToS3(file, s3Key);

        // 3. DB 저장 (expires_at = 1시간 후)
        Image image = Image.builder()
                .imageUrl(imageUrl)
                .fileSize((int) file.getSize())
                .originalFilename(file.getOriginalFilename())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        Image savedImage = imageRepository.save(image);
        log.info("Image uploaded: imageId={}, s3Key={}", savedImage.getImageId(), s3Key);

        return ImageResponse.from(savedImage);
    }

    /**
     * S3 업로드 수행
     *
     * @param file 업로드할 파일
     * @param s3Key S3 키
     * @return 이미지 URL
     */
    private String uploadToS3(MultipartFile file, String s3Key) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            // S3 URL 생성
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);

        } catch (IOException e) {
            log.error("Failed to upload image to S3: s3Key={}", s3Key, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("S3 upload error: s3Key={}", s3Key, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
