package com.ktb.community.session;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 세션 도메인 객체
 * - 세션 ID: UUID (쿠키에 저장)
 * - 유효시간: 1시간 고정
 * - 저장 위치: InMemorySessionStore (ConcurrentHashMap)
 */
@Getter
@Builder
public class Session {

    private String sessionId;      // UUID
    private Long userId;
    private String email;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;  // 1시간 고정

    /**
     * 세션 만료 여부 확인
     * @return true: 만료됨, false: 유효함
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
