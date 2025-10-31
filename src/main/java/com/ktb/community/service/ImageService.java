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
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
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
     * - 파일 검증 (MIME type, Magic Number)
     * - S3 업로드
     * - DB 저장 (expires_at = 1시간 후)
     */
    @Transactional
    public ImageResponse uploadImage(MultipartFile file) {
        log.debug("[Image] 이미지 업로드 시작: filename={}, size={}, contentType={}", 
            file.getOriginalFilename(), file.getSize(), file.getContentType());
        
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
        log.info("[Image] 이미지 업로드 완료: imageId={}, s3Key={}", savedImage.getImageId(), s3Key);

        return ImageResponse.from(savedImage);
    }

    /**
     * S3 업로드 수행
     * - PutObjectRequest 생성 및 S3 업로드
     * - 이미지 URL 반환
     */
    private String uploadToS3(MultipartFile file, String s3Key) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .acl(ObjectCannedACL.PUBLIC_READ)  // 이미지 객체만 public 설정
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            // S3 URL 생성
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);

        } catch (IOException e) {
            log.error("[Image] S3 업로드 실패 (IOException): s3Key={}", s3Key, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, 
                "S3 upload failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("[Image] S3 업로드 에러: s3Key={}", s3Key, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, 
                "S3 upload error: " + e.getMessage());
        }
    }
}
