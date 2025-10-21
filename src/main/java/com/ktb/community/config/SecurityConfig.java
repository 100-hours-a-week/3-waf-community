package com.ktb.community.config;

import com.ktb.community.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 설정
 * LLD.md Section 6.3 참조
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    /**
     * BCrypt 암호화 Bean 등록
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * AuthenticationManager Bean 등록
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS 설정 Bean (Express.js Frontend 연동)
     * - allowCredentials: true (httpOnly 쿠키 전송 허용)
     * - allowedOrigins: frontend.url 환경 변수
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);  // 쿠키 전송 허용
        config.setMaxAge(3600L);  // Preflight 캐싱 1시간

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
    
    /**
     * Security Filter Chain 설정
     * - JWT 기반 Stateless 인증
     * - CSRF 보호 활성화 (Cookie 기반, httpOnly=false for JavaScript access)
     * - 공개 엔드포인트와 인증 필요 엔드포인트 구분
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/auth/login", "/users/signup")  // 공개 엔드포인트 제외
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ========== 순서 중요: 구체적인 패턴 먼저! ==========

// 1. 특수 케이스 - GET이지만 인증 필요
                        .requestMatchers(HttpMethod.GET, "/posts/users/me/likes").authenticated()
                        
                        // 2. Public GET 엔드포인트
                        .requestMatchers(HttpMethod.GET, 
                                "/posts",                // 게시글 목록
                                "/posts/*",              // 게시글 상세
                                "/posts/*/comments",     // 댓글 목록
                                "/users/*"               // 사용자 프로필 (공개)
                        ).permitAll()
                        
                        // 3. 인증 필요 - Posts
                        .requestMatchers(HttpMethod.POST, "/posts").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/posts/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/posts/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/posts/*/like").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/posts/*/like").authenticated()
                        
                        // 4. 인증 필요 - Comments
                        .requestMatchers(HttpMethod.POST, "/posts/*/comments").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/posts/*/comments/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/posts/*/comments/*").authenticated()
                        
                        // 5. 인증 필요 - Users
                        .requestMatchers(HttpMethod.PATCH, "/users/*").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/users/*/password").authenticated()
                        
                        // 6. 인증 필요 - Images
                        .requestMatchers(HttpMethod.POST, "/images").authenticated()
                        
                        // 7. Public - Auth
                        .requestMatchers("/auth/login", "/auth/refresh_token", "/users/signup").permitAll()

                        // 8. Public - Legal & Static Resources
                        .requestMatchers("/terms", "/privacy", "/css/**").permitAll()

                        // 9. 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}

