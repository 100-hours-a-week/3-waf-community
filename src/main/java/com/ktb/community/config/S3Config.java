package com.ktb.community.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Value("${aws.profile}")
    private String profile;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * S3Client Bean 생성
     *
     * Credentials Provider:
     * - ProfileCredentialsProvider: ~/.aws/credentials 프로필 사용
     * - 프로필 이름: application.yaml의 aws.profile (기본값: dev)
     * - 로컬 개발: dev 프로필
     * - 배포 환경: 환경변수 AWS_PROFILE로 프로필 지정
     */
    @Bean
    public S3Client s3Client() {
        // application.yaml에서 주입받은 프로필 사용
        AwsCredentialsProvider credentialsProvider =
                ProfileCredentialsProvider.create(profile);

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
