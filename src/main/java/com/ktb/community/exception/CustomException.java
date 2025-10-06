package com.ktb.community.exception;

import lombok.Getter;

@Getter
public abstract class CustomException extends RuntimeException {
    
    private final String errorCode;
    
    protected CustomException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    protected CustomException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * 각 예외에 매핑되는 HTTP 상태 코드 반환
     * 하위 클래스에서 오버라이드
     */
    public abstract org.springframework.http.HttpStatus getHttpStatus();
}
