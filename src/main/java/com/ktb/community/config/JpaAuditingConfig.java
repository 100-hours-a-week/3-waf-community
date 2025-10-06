package com.ktb.community.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화 설정
 * BaseTimeEntity의 @CreatedDate, @LastModifiedDate 자동 관리
 *
 * 향후 작성자/수정자 추적이 필요한 경우:
 * - @CreatedBy, @LastModifiedBy 어노테이션 추가
 * - AuditorAware<Long> Bean 구현 (SecurityContext에서 사용자 ID 추출)
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // 시간 필드만 관리하는 경우 추가 설정 불필요
    // 향후 확장 예시:
    // @Bean
    // public AuditorAware<Long> auditorProvider() {
    //     return () -> Optional.ofNullable(SecurityContextHolder.getContext())
    //         .map(SecurityContext::getAuthentication)
    //         .filter(Authentication::isAuthenticated)
    //         .map(auth -> ((UserDetails) auth.getPrincipal()).getUserId());
    // }
}
