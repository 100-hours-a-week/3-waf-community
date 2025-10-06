package com.ktb.community.exception;

/**
 * 리소스 중복 시 발생하는 예외 (409)
 */
public class DuplicateResourceException extends CustomException {
    
    public DuplicateResourceException(String message) {
        super(message, "RESOURCE_ALREADY_EXISTS");
    }
    
    public DuplicateResourceException(String field, String value) {
        super(String.format("%s already exists: %s", field, value), "RESOURCE_ALREADY_EXISTS");
    }

    @Override
    public org.springframework.http.HttpStatus getHttpStatus() {
        return org.springframework.http.HttpStatus.CONFLICT;
    }
}
