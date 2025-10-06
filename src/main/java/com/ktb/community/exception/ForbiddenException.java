package com.ktb.community.exception;

/**
 * 권한 없음 시 발생하는 예외 (403)
 */
public class ForbiddenException extends CustomException {
    
    public ForbiddenException(String message) {
        super(message, "FORBIDDEN");
    }

    @Override
    public org.springframework.http.HttpStatus getHttpStatus() {
        return org.springframework.http.HttpStatus.FORBIDDEN;
    }
}
