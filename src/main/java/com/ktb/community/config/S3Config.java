package com.ktb.community.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS S3 클라이언트 설정
 * ProfileCredentialsProvider를 사용하여 ~/.aws/credentials의 프로파일 기반 인증
 */
@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.credentials.profile}")
    private String profile;

    /**
     * S3Client Bean 생성
     * - ProfileCredentialsProvider: ~/.aws/credentials의 [dev] 프로파일 사용
     * - Region: application.yaml의 aws.s3.region 값 사용 (기본: ap-northeast-2)
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(ProfileCredentialsProvider.create(profile))
                .build();
    }
}
