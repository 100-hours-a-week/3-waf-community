package com.ktb.community.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * S3 키 생성 유틸리티
 * 날짜 기반 디렉토리 구조 + UUID 파일명
 */
public class S3KeyGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * S3 키 생성
     * 형식: images/2025/10/11/{UUID}.{extension}
     *
     * @param originalFilename 원본 파일명
     * @return S3 키
     */
    public static String generateKey(String originalFilename) {
        String datePath = LocalDate.now().format(DATE_FORMATTER);
        String uuid = UUID.randomUUID().toString();
        String extension = extractExtension(originalFilename);
        
        return String.format("images/%s/%s%s", datePath, uuid, extension);
    }

    /**
     * 파일 확장자 추출 (점 포함)
     *
     * @param filename 파일명
     * @return 확장자 (예: ".jpg", ".png")
     */
    private static String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
