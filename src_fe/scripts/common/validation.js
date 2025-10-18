/**
 * Validation Utility
 * 폼 검증 유틸리티
 */

(function(window) {
  'use strict';

  const Validation = {
    /**
     * 이메일 검증
     */
    email(value) {
      if (!value) {
        return { valid: false, message: '이메일을 입력해주세요.' };
      }

      const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!regex.test(value)) {
        return { valid: false, message: '올바른 이메일 형식이 아닙니다.' };
      }

      return { valid: true };
    },

    /**
     * 비밀번호 검증
     * 8-20자, 영문 대소문자, 숫자, 특수문자 각 1개 이상
     */
    password(value) {
      if (!value) {
        return { valid: false, message: '비밀번호를 입력해주세요.' };
      }

      if (value.length < 8 || value.length > 20) {
        return {
          valid: false,
          message: '비밀번호는 8자 이상, 20자 이하여야 합니다.'
        };
      }

      const hasUpper = /[A-Z]/.test(value);
      const hasLower = /[a-z]/.test(value);
      const hasNumber = /[0-9]/.test(value);
      const hasSpecial = /[!@#$%^&*(),.?":{}|<>]/.test(value);

      if (!(hasUpper && hasLower && hasNumber && hasSpecial)) {
        return {
          valid: false,
          message: '비밀번호는 영문 대소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다.'
        };
      }

      return { valid: true };
    },

    /**
     * 비밀번호 강도 계산
     * @returns {string} 'weak', 'medium', 'strong'
     */
    passwordStrength(value) {
      if (!value) return 'weak';

      let strength = 0;

      if (value.length >= 8) strength++;
      if (value.length >= 12) strength++;
      if (/[A-Z]/.test(value)) strength++;
      if (/[a-z]/.test(value)) strength++;
      if (/[0-9]/.test(value)) strength++;
      if (/[!@#$%^&*(),.?":{}|<>]/.test(value)) strength++;

      if (strength <= 2) return 'weak';
      if (strength <= 4) return 'medium';
      return 'strong';
    },

    /**
     * 비밀번호 확인
     */
    passwordMatch(password, confirmPassword) {
      if (!confirmPassword) {
        return { valid: false, message: '비밀번호를 입력해주세요.' };
      }

      if (password !== confirmPassword) {
        return { valid: false, message: '비밀번호와 다릅니다.' };
      }

      return { valid: true };
    },

    /**
     * 닉네임 검증
     * 2-10자
     */
    nickname(value) {
      if (!value) {
        return { valid: false, message: '닉네임을 입력해주세요.' };
      }

      if (value.length < 2 || value.length > 10) {
        return {
          valid: false,
          message: '닉네임은 2자 이상, 10자 이하여야 합니다.'
        };
      }

      return { valid: true };
    },

    /**
     * 제목 검증
     * 최대 26자
     */
    title(value, maxLength = 26) {
      if (!value) {
        return { valid: false, message: '제목을 입력해주세요.' };
      }

      if (value.length > maxLength) {
        return {
          valid: false,
          message: `제목은 최대 ${maxLength}자까지 입력 가능합니다.`
        };
      }

      return { valid: true };
    },

    /**
     * 내용 검증
     */
    content(value) {
      if (!value) {
        return { valid: false, message: '내용을 입력해주세요.' };
      }

      return { valid: true };
    },

    /**
     * 파일 검증
     */
    file(file, options = {}) {
      const {
        maxSize = 10 * 1024 * 1024, // 10MB
        allowedTypes = ['image/jpeg', 'image/png', 'image/gif']
      } = options;

      if (!file) {
        return { valid: true }; // 선택 사항
      }

      // 파일 크기 검증
      if (file.size > maxSize) {
        const maxSizeMB = maxSize / (1024 * 1024);
        return {
          valid: false,
          message: `파일 크기는 최대 ${maxSizeMB}MB까지 업로드 가능합니다.`
        };
      }

      // 파일 타입 검증
      if (!allowedTypes.includes(file.type)) {
        return {
          valid: false,
          message: '지원하지 않는 파일 형식입니다.'
        };
      }

      return { valid: true };
    },

    /**
     * 필수 입력 검증
     */
    required(value) {
      if (!value || (typeof value === 'string' && !value.trim())) {
        return { valid: false, message: '필수 입력 항목입니다.' };
      }

      return { valid: true };
    },

    /**
     * 폼 전체 검증
     * @param {Object} rules - { fieldName: validationFunction }
     * @param {Object} values - { fieldName: value }
     * @returns {Object} { valid: boolean, errors: {} }
     */
    validateForm(rules, values) {
      const errors = {};
      let valid = true;

      Object.keys(rules).forEach(fieldName => {
        const validationFn = rules[fieldName];
        const value = values[fieldName];
        const result = validationFn(value);

        if (!result.valid) {
          errors[fieldName] = result.message;
          valid = false;
        }
      });

      return { valid, errors };
    }
  };

  // Export
  window.Validation = Validation;

})(window);
