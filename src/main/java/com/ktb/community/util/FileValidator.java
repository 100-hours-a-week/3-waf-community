package com.ktb.community.util;

import com.ktb.community.enums.ErrorCode;
import com.ktb.community.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 이미지 파일 검증 유틸리티
 * MIME type과 Magic Number를 검증하여 실제 이미지 파일인지 확인
 */
public class FileValidator {

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/gif"
    );

    // Magic Number (파일 시그니처)
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF_MAGIC_87A = {0x47, 0x49, 0x46, 0x38, 0x37, 0x61}; // GIF87a
    private static final byte[] GIF_MAGIC_89A = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61}; // GIF89a

    /**
     * 이미지 파일 검증
     * 1. MIME type 검증
     * 2. Magic Number 검증
     *
     * @param file 업로드된 파일
     * @throws BusinessException 유효하지 않은 파일 형식
     */
    public static void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 1. MIME type 검증
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }

        // 2. Magic Number 검증
        try {
            byte[] bytes = file.getBytes();
            if (!isValidImageMagicNumber(bytes)) {
                throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
            }
        } catch (IOException e) {
            // 디버깅: 파일 읽기 실패 원인 로깅
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, 
                "Failed to read file bytes: " + e.getMessage());
        }
    }

    /**
     * Magic Number로 실제 이미지 파일인지 검증
     *
     * @param bytes 파일 바이트 배열
     * @return 유효한 이미지 여부
     */
    private static boolean isValidImageMagicNumber(byte[] bytes) {
        if (bytes == null || bytes.length < 8) {
            return false;
        }

        return startsWithMagic(bytes, JPEG_MAGIC)
                || startsWithMagic(bytes, PNG_MAGIC)
                || startsWithMagic(bytes, GIF_MAGIC_87A)
                || startsWithMagic(bytes, GIF_MAGIC_89A);
    }

    /**
     * 바이트 배열이 특정 Magic Number로 시작하는지 확인
     *
     * @param bytes 파일 바이트 배열
     * @param magic Magic Number 배열
     * @return 일치 여부
     */
    private static boolean startsWithMagic(byte[] bytes, byte[] magic) {
        if (bytes.length < magic.length) {
            return false;
        }

        for (int i = 0; i < magic.length; i++) {
            if (bytes[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }
}
