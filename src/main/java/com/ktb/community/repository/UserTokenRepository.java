package com.ktb.community.repository;

import com.ktb.community.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * UserToken 엔티티 Repository
 * LLD.md Section 6.1 참조
 */
@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
    
    /**
     * 토큰으로 UserToken 조회
     */
    Optional<UserToken> findByToken(String token);
    
    /**
     * 사용자 ID와 토큰으로 UserToken 조회
     */
    Optional<UserToken> findByUserUserIdAndToken(Long userId, String token);
    
    /**
     * 특정 토큰 삭제 (로그아웃)
     */
    void deleteByToken(String token);
    
    /**
     * 특정 사용자의 모든 토큰 삭제
     */
    void deleteByUserUserId(Long userId);
    
    /**
     * 만료된 토큰 삭제 (배치 작업용)
     */
    @Modifying
    @Query("DELETE FROM UserToken ut WHERE ut.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
}
