package com.ktb.community.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 에러 상세 정보 DTO
 * GlobalExceptionHandler에서 에러 응답 시 사용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDetails {
    
    /**
     * 에러가 발생한 필드명 목록 (유효성 검증 실패 시)
     */
    private List<String> field;
    
    /**
     * 에러 상세 메시지
     */
    private String details;
    
    /**
     * 필드 에러 생성 (유효성 검증 실패)
     * 
     * @param field 에러 필드명
     * @param details 상세 메시지
     * @return ErrorDetails 객체
     */
    public static ErrorDetails of(String field, String details) {
        return new ErrorDetails(List.of(field), details);
    }
    
    /**
     * 필드 에러 생성 (여러 필드)
     * 
     * @param fields 에러 필드명 목록
     * @param details 상세 메시지
     * @return ErrorDetails 객체
     */
    public static ErrorDetails of(List<String> fields, String details) {
        return new ErrorDetails(fields, details);
    }
    
    /**
     * 일반 에러 생성 (필드 없음)
     * 
     * @param details 상세 메시지
     * @return ErrorDetails 객체
     */
    public static ErrorDetails of(String details) {
        return new ErrorDetails(null, details);
    }
}
