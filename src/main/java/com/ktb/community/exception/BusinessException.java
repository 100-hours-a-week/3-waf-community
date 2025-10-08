package com.ktb.community.exception;

import com.ktb.community.enums.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 로직 예외 통합 클래스
 * ErrorCode enum으로 에러 정보 관리
 * 
 * 사용 예시:
 * throw new BusinessException(ErrorCode.USER_NOT_FOUND);
 * throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, "email: " + email);
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    /**
     * ErrorCode의 기본 메시지 사용
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    /**
     * 커스텀 메시지로 기본 메시지 오버라이드
     */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
    
    /**
     * 원인 예외와 함께 생성
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
    
    /**
     * 커스텀 메시지와 원인 예외 함께 생성
     */
    public BusinessException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * HTTP 상태 코드 반환
     */
    public HttpStatus getHttpStatus() {
        return errorCode.getHttpStatus();
    }
    
    /**
     * 에러 코드 문자열 반환 (예: "USER-001")
     */
    public String getErrorCodeString() {
        return errorCode.getCode();
    }
}
