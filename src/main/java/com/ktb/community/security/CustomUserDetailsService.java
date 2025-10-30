/*
 * [Spring Security 제거] 기존 구현 (보존)
 *
 * Spring Security UserDetailsService 구현
 * → JWT 마이그레이션 후 사용하지 않음 (순수 Servlet Filter는 UserDetails 불필요)
 *
 * 주석처리 이유:
 * - Spring Security 의존성 제거 (UserDetailsService, UserDetails)
 * - JwtAuthenticationFilter (순수 Filter)는 Request Attribute만 사용
 *
 * 보존 이유:
 * - 향후 Spring Security 전환 시 참고용
 * - UserDetails 변환 패턴 학습 자료
 */

/*
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

    public UserDetails loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

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
*/
