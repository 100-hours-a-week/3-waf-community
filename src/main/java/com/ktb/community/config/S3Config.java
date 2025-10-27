package com.ktb.community.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;

    /**
     * S3Client Bean 생성
     *
     * Credentials Provider 선택 전략:
     * 1. AWS_PROFILE 환경변수 존재 → ProfileCredentialsProvider (로컬 개발 환경)
     * 2. AWS_PROFILE 없음 → DefaultCredentialsProvider (배포 환경)
     *    - 환경변수 (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
     *    - System Properties
     *    - ~/.aws/credentials ([default] 프로파일)
     *    - IAM Role (EC2, ECS, Lambda)
     */
    @Bean
    public S3Client s3Client() {
        String profile = System.getenv("AWS_PROFILE");

        AwsCredentialsProvider credentialsProvider;
        if (profile != null && !profile.isEmpty()) {
            // 로컬 개발: AWS_PROFILE 환경변수 사용 (예: dev, prod)
            credentialsProvider = ProfileCredentialsProvider.create(profile);
        } else {
            // 배포 환경: DefaultCredentialsProvider (환경변수 또는 IAM Role)
            credentialsProvider = DefaultCredentialsProvider.create();
        }

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
