package com.ktb.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security 관련 설정
 * 비밀번호 암호화 및 인증 설정
 */
@Configuration
public class SecurityConfig {
    
    /**
     * BCrypt 암호화 Bean 등록
     * 사용자 비밀번호 암호화에 사용
     * 
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
