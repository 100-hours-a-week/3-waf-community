package com.ktb.community.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PasswordValidator 단위 테스트
 * 비밀번호 정책: 8-20자, 대문자/소문자/특수문자 각 1개 이상
 */
@DisplayName("PasswordValidator 테스트")
class PasswordValidatorTest {

    @Test
    @DisplayName("유효한 비밀번호 - 검증 성공")
    void isValid_ValidPassword_ReturnsTrue() {
        // Given
        String validPassword = "Test1234!";

        // When
        boolean result = PasswordValidator.isValid(validPassword);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("너무 짧은 비밀번호 (8자 미만) - 검증 실패")
    void isValid_TooShort_ReturnsFalse() {
        // Given
        String shortPassword = "Test1!";

        // When
        boolean result = PasswordValidator.isValid(shortPassword);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("너무 긴 비밀번호 (20자 초과) - 검증 실패")
    void isValid_TooLong_ReturnsFalse() {
        // Given
        String longPassword = "Test1234!Test1234!Test1234!";

        // When
        boolean result = PasswordValidator.isValid(longPassword);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("대문자 없음 - 검증 실패")
    void isValid_NoUppercase_ReturnsFalse() {
        // Given
        String noUppercasePassword = "test1234!";

        // When
        boolean result = PasswordValidator.isValid(noUppercasePassword);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("소문자 없음 - 검증 실패")
    void isValid_NoLowercase_ReturnsFalse() {
        // Given
        String noLowercasePassword = "TEST1234!";

        // When
        boolean result = PasswordValidator.isValid(noLowercasePassword);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("특수문자 없음 - 검증 실패")
    void isValid_NoSpecialChar_ReturnsFalse() {
        // Given
        String noSpecialCharPassword = "Test12345";

        // When
        boolean result = PasswordValidator.isValid(noSpecialCharPassword);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("null 입력 - 검증 실패")
    void isValid_NullInput_ReturnsFalse() {
        // Given
        String nullPassword = null;

        // When
        boolean result = PasswordValidator.isValid(nullPassword);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("빈 문자열 입력 - 검증 실패")
    void isValid_EmptyString_ReturnsFalse() {
        // Given
        String emptyPassword = "";

        // When
        boolean result = PasswordValidator.isValid(emptyPassword);

        // Then
        assertThat(result).isFalse();
    }
}
