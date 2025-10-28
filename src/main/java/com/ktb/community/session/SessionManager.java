package com.ktb.community.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 세션 관리 매니저
 * - 세션 생성, 조회, 삭제
 * - 세션 ID는 UUID v4 (2^122 가능성, 충돌 없음)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionManager {

    private final InMemorySessionStore sessionStore;

    /**
     * 세션 생성 (로그인)
     * @param userId 사용자 ID
     * @param email 이메일
     * @param role 권한 (USER/ADMIN)
     * @return 세션 ID (UUID)
     */
    public String createSession(Long userId, String email, String role) {
        String sessionId = UUID.randomUUID().toString();

        Session session = Session.builder()
                .sessionId(sessionId)
                .userId(userId)
                .email(email)
                .role(role)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))  // 1시간 고정
                .build();

        sessionStore.save(session);
        log.info("[SessionManager] 세션 생성 완료");

        return sessionId;
    }

    /**
     * 세션 조회
     * @param sessionId 세션 ID
     * @return Session (만료 시 빈 Optional)
     */
    public Optional<Session> findById(String sessionId) {
        return sessionStore.findById(sessionId);
    }

    /**
     * 세션 삭제 (로그아웃)
     * @param sessionId 세션 ID
     */
    public void deleteSession(String sessionId) {
        sessionStore.deleteById(sessionId);
        log.info("[SessionManager] 세션 삭제 완료");
    }
}
