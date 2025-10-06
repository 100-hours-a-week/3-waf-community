package com.ktb.community.exception;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외 (404)
 */
public class ResourceNotFoundException extends CustomException {
    
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
    }
    
    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s not found with id: %d", resourceName, id), "RESOURCE_NOT_FOUND");
    }

    @Override
    public org.springframework.http.HttpStatus getHttpStatus() {
        return org.springframework.http.HttpStatus.NOT_FOUND;
    }
}
