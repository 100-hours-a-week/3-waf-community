package com.ktb.community.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rate Limiting 어노테이션
 * Controller 메서드에 적용하여 API 호출 빈도 제한
 * 
 * 사용 예시:
 * @RateLimit(requestsPerMinute = 100)
 * public ResponseEntity login(@RequestBody LoginRequest request) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * 분당 허용 요청 수
     * 기본값: 100회/분
     */
    int requestsPerMinute() default 100;
}
