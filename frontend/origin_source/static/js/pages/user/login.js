/**
 * Login Page Script
 * 로그인 페이지 로직
 * 참조: @CLAUDE.md Section 4.4, @docs/be/API.md Section 1.1
 */

(function(window, document) {
    'use strict';

    // ============================================
    // Configuration
    // ============================================
    const CONFIG = {
        API_BASE_URL: 'http://localhost:8080',
        LIST_URL: '/pages/board/list.html',
        LOGIN_URL: '/pages/user/login.html'
    };

    // ============================================
    // State Management
    // ============================================
    const state = {
        isSubmitting: false  // 중복 제출 방지 플래그
    };

    // ============================================
    // DOM Element Caching
    // ============================================
    const elements = {
        form: null,
        emailInput: null,
        passwordInput: null,
        submitButton: null,
        emailError: null,
        passwordError: null
    };

    /**
     * 초기화
     */
    async function init() {
        // userId가 있으면 쿠키 유효성 검증
        if (isAuthenticated()) {
            const isValid = await verifySession();
            if (isValid) {
                // 유효한 세션 → 게시글 목록으로 리다이렉트
                window.location.replace(CONFIG.LIST_URL);
                return;
            }
            // 무효한 세션 → userId 정리 (stale state)
            localStorage.removeItem('userId');
        }

        cacheElements();
        bindEvents();
    }

    /**
     * 세션 유효성 검증 (쿠키 확인)
     * @returns {Promise<boolean>}
     */
    async function verifySession() {
        try {
            // 간단한 인증 API 호출로 쿠키 유효성 확인
            const response = await fetch(`${CONFIG.API_BASE_URL}/posts?limit=1`, {
                credentials: 'include'
            });
            return response.ok;
        } catch (error) {
            return false;
        }
    }

    /**
     * DOM 요소 캐싱
     */
    function cacheElements() {
        elements.form = document.querySelector('[data-form="login"]');
        elements.emailInput = document.querySelector('[data-field="email"]');
        elements.passwordInput = document.querySelector('[data-field="password"]');
        elements.submitButton = document.querySelector('[data-action="login"]');
        elements.emailError = document.querySelector('[data-error="email"]');
        elements.passwordError = document.querySelector('[data-error="password"]');
    }

    /**
     * 이벤트 바인딩
     */
    function bindEvents() {
        elements.form.addEventListener('submit', handleSubmit);

        // 실시간 검증 (blur 이벤트)
        elements.emailInput.addEventListener('blur', () => validateField('email'));
        elements.passwordInput.addEventListener('blur', () => validateField('password'));

        // 입력 시 에러 메시지 제거
        elements.emailInput.addEventListener('input', () => clearError('email'));
        elements.passwordInput.addEventListener('input', () => clearError('password'));
    }

    /**
     * 폼 제출 핸들러
     * POST /auth/login
     */
    async function handleSubmit(event) {
        event.preventDefault();

        // 중복 제출 방지
        if (state.isSubmitting) {
            return;
        }

        // 폼 검증
        if (!validateForm()) {
            return;
        }

        const email = sanitizeInput(elements.emailInput.value);
        const password = elements.passwordInput.value;

        try {
            state.isSubmitting = true;
            setLoading(true);

            // API 호출 (서버가 HttpOnly Cookie 설정)
            const response = await fetch(`${CONFIG.API_BASE_URL}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ email, password })
            });

            const result = await response.json();

            if (response.ok) {
                // userId만 localStorage에 저장 (토큰은 Cookie)
                if (result.data && result.data.userId) {
                    localStorage.setItem('userId', result.data.userId);
                }

                // 게시글 목록으로 리다이렉트 (replace로 히스토리에서 로그인 페이지 제거)
                window.location.replace(CONFIG.LIST_URL);
            } else {
                throw new Error(result.message);
            }

        } catch (error) {
            handleLoginError(error);
        } finally {
            state.isSubmitting = false;
            setLoading(false);
        }
    }

    /**
     * 개별 필드 검증 (blur 이벤트용)
     * @param {string} field - 'email' | 'password'
     * @returns {boolean}
     */
    function validateField(field) {
        clearError(field);

        if (field === 'email') {
            const email = sanitizeInput(elements.emailInput.value);

            if (!email) {
                showError('email', '이메일을 입력해주세요.');
                return false;
            } else if (hasExcessiveWhitespace(elements.emailInput.value)) {
                showError('email', '이메일에 공백이 너무 많습니다.');
                return false;
            } else if (!isValidEmail(email)) {
                showError('email', '올바른 이메일 형식이 아닙니다.');
                return false;
            }
            return true;
        }

        if (field === 'password') {
            const password = elements.passwordInput.value;

            if (!password) {
                showError('password', '비밀번호를 입력해주세요.');
                return false;
            }
            return true;
        }

        return true;
    }

    /**
     * 폼 유효성 검증
     * @returns {boolean}
     */
    function validateForm() {
        let isValid = true;

        // 모든 필드 검증
        if (!validateField('email')) isValid = false;
        if (!validateField('password')) isValid = false;

        return isValid;
    }

    /**
     * 로그인 에러 처리
     * @param {Error} error
     */
    function handleLoginError(error) {
        const message = error.message || '';

        // ErrorCode 번역 (translateErrorCode는 api.js에 있음)
        const translatedMessage = translateErrorCode(message);

        // AUTH-001: 이메일 또는 비밀번호 불일치
        if (message.includes('AUTH-001')) {
            showError('password', translatedMessage);
        }
        // USER-005: 계정 비활성화
        else if (message.includes('USER-005')) {
            showError('email', translatedMessage);
        }
        // 기타 에러
        else {
            showError('password', translatedMessage || '로그인에 실패했습니다.');
        }
    }

    /**
     * 에러 메시지 표시
     * @param {string} field - 'email' | 'password'
     * @param {string} message
     */
    function showError(field, message) {
        const errorElement = field === 'email' ? elements.emailError : elements.passwordError;
        const inputElement = field === 'email' ? elements.emailInput : elements.passwordInput;

        if (errorElement) {
            errorElement.textContent = message;
            errorElement.style.display = 'block';
        }

        if (inputElement) {
            inputElement.classList.add('input-field__input--error');
        }
    }

    /**
     * 에러 메시지 제거
     * @param {string} field - 'email' | 'password'
     */
    function clearError(field) {
        const errorElement = field === 'email' ? elements.emailError : elements.passwordError;
        const inputElement = field === 'email' ? elements.emailInput : elements.passwordInput;

        if (errorElement) {
            errorElement.textContent = '';
            errorElement.style.display = 'none';
        }

        if (inputElement) {
            inputElement.classList.remove('input-field__input--error');
        }
    }

    /**
     * 로딩 상태 설정
     * @param {boolean} loading
     */
    function setLoading(loading) {
        if (loading) {
            elements.submitButton.disabled = true;
            elements.submitButton.textContent = '로그인 중...';
        } else {
            elements.submitButton.disabled = false;
            elements.submitButton.textContent = '로그인';
        }
    }

    // 초기화
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})(window, document);
