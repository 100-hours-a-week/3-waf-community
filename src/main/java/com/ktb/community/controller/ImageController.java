package com.ktb.community.controller;

import com.ktb.community.config.RateLimit;
import com.ktb.community.dto.ApiResponse;
import com.ktb.community.dto.response.ImageResponse;
import com.ktb.community.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 업로드 컨트롤러
 * API.md Section 4 참조
 */
@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    /**
     * 이미지 업로드 (API.md Section 4.1)
     * POST /images
     * Authorization: Bearer {access_token}
     * Tier 2: 중간 제한 (파일 업로드 부하)
     *
     * @param file 업로드할 이미지 파일
     * @return 이미지 정보 (image_id, image_url)
     */
    @PostMapping
    @RateLimit(requestsPerMinute = 10)
    public ResponseEntity<ApiResponse<ImageResponse>> uploadImage(
            @RequestParam("file") MultipartFile file) {

        ImageResponse imageResponse = imageService.uploadImage(file);

        ApiResponse<ImageResponse> response = ApiResponse.success(
                "upload_image_success",
                imageResponse
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
