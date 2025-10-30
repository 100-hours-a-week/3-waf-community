package com.ktb.community.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인메모리 세션 저장소
 * - ConcurrentHashMap 사용 (Thread-Safe) - 세분화된 잠금 (Fine-Grained Locking)
 * - 만료된 세션 자동 정리 (조회 시)
 */
@Slf4j
@Component
public class InMemorySessionStore {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /**
     * 세션 저장
     */
    public void save(Session session) {
        sessions.put(session.getSessionId(), session);
        log.debug("[SessionStore] 세션 저장 완료");
    }

    /**
     * 세션 조회 (만료 시 자동 삭제)
     */
    public Optional<Session> findById(String sessionId) {
        Session session = sessions.get(sessionId);

        if (session == null) {
            return Optional.empty();
        }

        // 만료 확인
        if (session.isExpired()) {
            sessions.remove(sessionId);
            log.debug("[SessionStore] 만료된 세션 자동 삭제");
            return Optional.empty();
        }

        return Optional.of(session);
    }

    /**
     * 세션 삭제 (로그아웃)
     */
    public void deleteById(String sessionId) {
        Session removed = sessions.remove(sessionId);
        if (removed != null) {
            log.debug("[SessionStore] 세션 삭제 완료");
        }
    }

    /**
     * 전체 세션 개수 (디버깅용)
     */
    public int size() {
        return sessions.size();
    }
}
