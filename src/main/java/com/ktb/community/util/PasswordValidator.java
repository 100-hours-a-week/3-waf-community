package com.ktb.community.util;

import java.util.regex.Pattern;

/**
 * 비밀번호 정책 검증 유틸리티
 * LLD.md Section 6.4, PRD.md FR-AUTH-001 참조
 * 
 * 정책:
 * - 길이: 8-20자
 * - 대문자: 최소 1개
 * - 소문자: 최소 1개
 * - 특수문자: 최소 1개
 */
public class PasswordValidator {

    private static final String PASSWORD_PATTERN = 
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?\":{}|<>]).{8,20}$";
    
    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    /**
     * 비밀번호 유효성 검증
     * @param password 검증할 비밀번호
     * @return 유효하면 true
     */
    public static boolean isValid(String password) {
        if (password == null) {
            return false;
        }
        return pattern.matcher(password).matches();
    }

    /**
     * 비밀번호 정책 설명 반환
     */
    public static String getPolicyDescription() {
        return "비밀번호는 8-20자이며, 대문자, 소문자, 특수문자를 각각 최소 1개 포함해야 합니다.";
    }
}
