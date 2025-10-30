package com.ktb.community.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 설정 (WebMvcConfigurer)
 * - Spring Security 제거 후 WebMvc로 CORS 설정 이동
 * - Express.js Frontend 연동
 * - httpOnly Cookie 전송 허용 (allowCredentials: true)
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(frontendUrl)
                .allowedMethods("GET", "POST", "PATCH", "DELETE","PUT","OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)  // httpOnly Cookie 전송 허용
                .maxAge(3600);  // Preflight 캐싱 1시간
    }
}
