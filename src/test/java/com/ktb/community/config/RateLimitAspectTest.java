package com.ktb.community.config;

import com.ktb.community.enums.ErrorCode;
import com.ktb.community.exception.BusinessException;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RateLimitAspect 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitAspect 테스트")
class RateLimitAspectTest {

    @InjectMocks
    private RateLimitAspect rateLimitAspect;

    @Mock
    private ProceedingJoinPoint pjp;

    @Mock
    private Signature signature;

    @Mock
    private HttpServletRequest request;

    @Mock
    private RateLimit rateLimit;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        // RequestContextHolder 설정
        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);

        // SecurityContext 설정
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);
    }

    @Test
    @DisplayName("정상 요청 - Rate limit 내에서 요청 성공")
    void rateLimit_Success() throws Throwable {
        // Given
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.ktb.community.controller.AuthController");
        when(signature.getName()).thenReturn("login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimit.requestsPerMinute()).thenReturn(100);
        when(pjp.proceed()).thenReturn("success");

        // When
        Object result = rateLimitAspect.rateLimit(pjp, rateLimit);

        // Then
        assertThat(result).isEqualTo("success");
        verify(pjp, times(1)).proceed();
    }

    @Test
    @DisplayName("Rate limit 초과 - TOO_MANY_REQUESTS 예외 발생")
    void rateLimit_ExceedsLimit() throws Throwable {
        // Given
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.ktb.community.controller.AuthController");
        when(signature.getName()).thenReturn("login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimit.requestsPerMinute()).thenReturn(2); // 2회만 허용

        // When & Then
        // 첫 번째, 두 번째 요청은 성공
        assertThatThrownBy(() -> {
            rateLimitAspect.rateLimit(pjp, rateLimit);
            rateLimitAspect.rateLimit(pjp, rateLimit);
            // 세 번째 요청은 실패
            rateLimitAspect.rateLimit(pjp, rateLimit);
        }).isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("다른 메서드 독립성 - 같은 IP라도 다른 메서드는 독립적인 카운터")
    void rateLimit_DifferentMethods_IndependentCounters() throws Throwable {
        // Given
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.ktb.community.controller.AuthController");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimit.requestsPerMinute()).thenReturn(2);
        when(pjp.proceed()).thenReturn("success");

        // When - login 메서드 2회 호출
        when(signature.getName()).thenReturn("login");
        rateLimitAspect.rateLimit(pjp, rateLimit);
        rateLimitAspect.rateLimit(pjp, rateLimit);

        // Then - signup 메서드는 독립적이므로 성공해야 함
        when(signature.getName()).thenReturn("signup");
        Object result = rateLimitAspect.rateLimit(pjp, rateLimit);

        assertThat(result).isEqualTo("success");
    }

    @Test
    @DisplayName("인증 사용자 - IP:userId 키 사용")
    void rateLimit_AuthenticatedUser_UsesUserIdInKey() throws Throwable {
        // Given
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.ktb.community.controller.PostController");
        when(signature.getName()).thenReturn("createPost");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimit.requestsPerMinute()).thenReturn(2);
        when(pjp.proceed()).thenReturn("success");

        // 인증 사용자 설정
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("user");
        when(authentication.getName()).thenReturn("user@example.com");

        // When - 인증 사용자로 2회 호출
        rateLimitAspect.rateLimit(pjp, rateLimit);
        rateLimitAspect.rateLimit(pjp, rateLimit);

        // Then - 3회째는 실패해야 함
        assertThatThrownBy(() -> rateLimitAspect.rateLimit(pjp, rateLimit))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("비인증 사용자 - IP만 사용")
    void rateLimit_UnauthenticatedUser_UsesIpOnly() throws Throwable {
        // Given
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.ktb.community.controller.PostController");
        when(signature.getName()).thenReturn("getPosts");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimit.requestsPerMinute()).thenReturn(2);
        when(pjp.proceed()).thenReturn("success");

        // 비인증 사용자 (authentication null)
        when(securityContext.getAuthentication()).thenReturn(null);

        // When - 비인증 사용자로 2회 호출
        rateLimitAspect.rateLimit(pjp, rateLimit);
        rateLimitAspect.rateLimit(pjp, rateLimit);

        // Then - 3회째는 실패해야 함
        assertThatThrownBy(() -> rateLimitAspect.rateLimit(pjp, rateLimit))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("IP 추출 - X-Forwarded-For 헤더 우선")
    void rateLimit_IpExtraction_XForwardedFor() throws Throwable {
        // Given
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.ktb.community.controller.AuthController");
        when(signature.getName()).thenReturn("login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");
        // X-Forwarded-For가 있으면 getRemoteAddr()는 호출되지 않음
        when(rateLimit.requestsPerMinute()).thenReturn(100);
        when(pjp.proceed()).thenReturn("success");

        // When
        Object result = rateLimitAspect.rateLimit(pjp, rateLimit);

        // Then
        assertThat(result).isEqualTo("success");
        verify(request, times(1)).getHeader("X-Forwarded-For");
    }

    @Test
    @DisplayName("IP 추출 - X-Forwarded-For 복수 IP 처리")
    void rateLimit_IpExtraction_XForwardedFor_MultipleIps() throws Throwable {
        // Given
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.ktb.community.controller.AuthController");
        when(signature.getName()).thenReturn("login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2, 10.0.0.3");
        when(rateLimit.requestsPerMinute()).thenReturn(2);
        when(pjp.proceed()).thenReturn("success");

        // When - 첫 번째 IP로 2회 호출
        rateLimitAspect.rateLimit(pjp, rateLimit);
        rateLimitAspect.rateLimit(pjp, rateLimit);

        // Then - 3회째는 실패해야 함 (같은 첫 번째 IP 10.0.0.1 사용)
        assertThatThrownBy(() -> rateLimitAspect.rateLimit(pjp, rateLimit))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("IP 추출 - Proxy-Client-IP 헤더 fallback")
    void rateLimit_IpExtraction_ProxyClientIp() throws Throwable {
        // Given
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.ktb.community.controller.AuthController");
        when(signature.getName()).thenReturn("login");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn("10.0.0.2");
        when(rateLimit.requestsPerMinute()).thenReturn(100);
        when(pjp.proceed()).thenReturn("success");

        // When
        Object result = rateLimitAspect.rateLimit(pjp, rateLimit);

        // Then
        assertThat(result).isEqualTo("success");
        verify(request, times(1)).getHeader("Proxy-Client-IP");
    }

    @Test
    @DisplayName("IP 추출 - RemoteAddr fallback")
    void rateLimit_IpExtraction_RemoteAddr() throws Throwable {
        // Given
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.ktb.community.controller.AuthController");
        when(signature.getName()).thenReturn("login");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimit.requestsPerMinute()).thenReturn(100);
        when(pjp.proceed()).thenReturn("success");

        // When
        Object result = rateLimitAspect.rateLimit(pjp, rateLimit);

        // Then
        assertThat(result).isEqualTo("success");
        verify(request, times(1)).getRemoteAddr();
    }

    @Test
    @DisplayName("FQCN 키 형식 - 패키지 경로 포함")
    void rateLimit_KeyFormat_UsesFQCN() throws Throwable {
        // Given
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.ktb.community.controller.AuthController");
        when(signature.getName()).thenReturn("login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimit.requestsPerMinute()).thenReturn(2);
        when(pjp.proceed()).thenReturn("success");

        // When - AuthController.login으로 2회 호출
        rateLimitAspect.rateLimit(pjp, rateLimit);
        rateLimitAspect.rateLimit(pjp, rateLimit);

        // Then - 다른 패키지의 AuthController.login은 독립적이어야 함
        when(signature.getDeclaringTypeName()).thenReturn("com.ktb.community.admin.controller.AuthController");
        Object result = rateLimitAspect.rateLimit(pjp, rateLimit);

        assertThat(result).isEqualTo("success");
    }
}
