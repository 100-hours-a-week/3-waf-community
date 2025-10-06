package com.ktb.community.exception;

/**
 * 잘못된 요청 시 발생하는 예외 (400)
 */
public class InvalidRequestException extends CustomException {
    
    public InvalidRequestException(String message) {
        super(message, "INVALID_REQUEST");
    }

    @Override
    public org.springframework.http.HttpStatus getHttpStatus() {
        return org.springframework.http.HttpStatus.BAD_REQUEST;
    }
}
