package com.ktb.community.security;

import com.ktb.community.entity.User;
import com.ktb.community.enums.UserStatus;
import com.ktb.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * [세션 전환]
 * 현재는 세션 기반 인증(SessionAuthenticationFilter)을 사용하므로 이 서비스는 실행되지 않습니다.
 * (JwtAuthenticationFilter에서만 사용되며, 해당 필터는 비활성화됨)
 * JWT 인프라는 향후 토큰 기반 인증으로 전환 시 재사용을 위해 보존되었습니다.
 * 
 * 관련 파일:
 * - SessionAuthenticationFilter: 세션 쿠키 검증 및 인증 처리
 * - SessionManager: 세션 생성/조회/삭제
 * - InMemorySessionStore: 세션 저장소 (ConcurrentHashMap)
 */

/**
 * Spring Security UserDetailsService 구현
 * 이메일로 사용자 인증 정보를 로드
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAndUserStatus(email.toLowerCase().trim(), UserStatus.ACTIVE)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(user.getUserStatus() != UserStatus.ACTIVE)
                .build();
    }
    
    /**
     * User ID로 사용자 로드 (JWT 검증용)
     */
    public UserDetails loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        
        // username을 userId로 설정 (JWT subject와 일치)
        return org.springframework.security.core.userdetails.User.builder()
                .username(String.valueOf(user.getUserId()))
                .password(user.getPasswordHash())
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(user.getUserStatus() != UserStatus.ACTIVE)
                .build();
    }
}
