package com.ktb.community.repository;

import com.ktb.community.entity.User;
import com.ktb.community.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    /**
     * 이메일로 사용자 조회 (프로필 이미지 Fetch Join)
     * LazyInitializationException 방지용
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profileImage WHERE u.email = :email")
    Optional<User> findByEmailWithProfileImage(@Param("email") String email);

    /**
     * 상태별 사용자 수 조회
     */
    long countByUserStatus(UserStatus status);
}
