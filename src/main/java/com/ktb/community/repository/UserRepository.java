package com.ktb.community.repository;

import com.ktb.community.entity.User;
import com.ktb.community.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User 엔티티 Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 닉네임으로 사용자 조회
     */
    Optional<User> findByNickname(String nickname);
    
    /**
     * 이메일 중복 확인
     */
    boolean existsByEmail(String email);
    
    /**
     * 닉네임 중복 확인
     */
    boolean existsByNickname(String nickname);
    
    /**
     * 이메일과 상태로 사용자 조회
     */
    Optional<User> findByEmailAndUserStatus(String email, UserStatus userStatus);
    
    /**
     * 사용자 ID와 상태로 조회
     */
    Optional<User> findByUserIdAndUserStatus(Long userId, UserStatus userStatus);
    
    /**
     * 이메일과 상태 목록으로 사용자 조회 (ACTIVE + INACTIVE 등)
     */
    Optional<User> findByEmailAndUserStatusIn(String email, java.util.List<UserStatus> statuses);
    
    /**
     * 사용자 ID와 상태 목록으로 존재 확인 (ACTIVE + INACTIVE 등)
     */
    boolean existsByUserIdAndUserStatusIn(Long userId, java.util.List<UserStatus> statuses);
}
