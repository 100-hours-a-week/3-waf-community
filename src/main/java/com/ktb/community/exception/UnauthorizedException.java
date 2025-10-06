package com.ktb.community.exception;

/**
 * 인증 실패 시 발생하는 예외 (401)
 */
public class UnauthorizedException extends CustomException {
    
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED");
    }

    @Override
    public org.springframework.http.HttpStatus getHttpStatus() {
        return org.springframework.http.HttpStatus.UNAUTHORIZED;
    }
}
