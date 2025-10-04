package com.ktb.community.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 공통 API 응답 구조
 * 모든 REST API 응답에 사용되는 표준 포맷
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    /**
     * 작업 결과 메시지 (예: "login_success", "get_user_success")
     */
    private String message;
    
    /**
     * 응답 데이터 (성공 시 데이터, 실패 시 null 또는 에러 상세)
     */
    private T data;
    
    /**
     * 응답 생성 시간
     */
    private LocalDateTime timestamp;
    
    /**
     * 성공 응답 생성 (데이터 포함)
     * 
     * @param message 성공 메시지
     * @param data 응답 데이터
     * @return ApiResponse 객체
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(message, data, LocalDateTime.now());
    }
    
    /**
     * 성공 응답 생성 (데이터 없음)
     * 
     * @param message 성공 메시지
     * @return ApiResponse 객체
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(message, null, LocalDateTime.now());
    }
    
    /**
     * 실패 응답 생성 (에러 상세 포함)
     * 
     * @param message 에러 메시지
     * @param errorDetails 에러 상세 정보
     * @return ApiResponse 객체
     */
    public static <T> ApiResponse<T> error(String message, T errorDetails) {
        return new ApiResponse<>(message, errorDetails, LocalDateTime.now());
    }
    
    /**
     * 실패 응답 생성 (에러 상세 없음)
     * 
     * @param message 에러 메시지
     * @return ApiResponse 객체
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(message, null, LocalDateTime.now());
    }
}
