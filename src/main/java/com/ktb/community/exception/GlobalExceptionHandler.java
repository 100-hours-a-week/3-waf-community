package com.ktb.community.exception;

import com.ktb.community.dto.ApiResponse;
import com.ktb.community.dto.ErrorDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 * 모든 컨트롤러에서 발생하는 예외를 중앙에서 처리
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 유효성 검증 실패 예외 처리 (@Valid, @Validated)
     * 
     * @param ex MethodArgumentNotValidException
     * @return 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        List<String> fields = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getField)
                .collect(Collectors.toList());
        
        String details = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("Validation failed for fields: {} - {}", fields, details);
        
        ErrorDetails errorDetails = ErrorDetails.of(fields, details);
        ApiResponse<ErrorDetails> response = ApiResponse.error("input_data_validation_failed", errorDetails);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * IllegalArgumentException 예외 처리 (잘못된 요청 파라미터)
     * 
     * @param ex IllegalArgumentException
     * @return 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        
        log.warn("Illegal argument: {}", ex.getMessage());
        
        ErrorDetails errorDetails = ErrorDetails.of(ex.getMessage());
        ApiResponse<ErrorDetails> response = ApiResponse.error("invalid_request", errorDetails);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * IllegalStateException 예외 처리 (비즈니스 로직 오류)
     * 
     * @param ex IllegalStateException
     * @return 400 Bad Request
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleIllegalStateException(
            IllegalStateException ex) {
        
        log.warn("Illegal state: {}", ex.getMessage());
        
        ErrorDetails errorDetails = ErrorDetails.of(ex.getMessage());
        ApiResponse<ErrorDetails> response = ApiResponse.error("invalid_state", errorDetails);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 일반 예외 처리 (예상하지 못한 서버 오류)
     * 
     * @param ex Exception
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleGeneralException(Exception ex) {
        
        log.error("Unexpected error occurred", ex);
        
        ErrorDetails errorDetails = ErrorDetails.of("An unexpected error occurred");
        ApiResponse<ErrorDetails> response = ApiResponse.error("internal_server_error", errorDetails);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * NullPointerException 예외 처리
     * 
     * @param ex NullPointerException
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleNullPointerException(
            NullPointerException ex) {
        
        log.error("Null pointer exception occurred", ex);
        
        ErrorDetails errorDetails = ErrorDetails.of("A required value was null");
        ApiResponse<ErrorDetails> response = ApiResponse.error("internal_server_error", errorDetails);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
