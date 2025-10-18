/**
 * Login Page Script
 * 로그인 페이지 로직
 * 참조: @CLAUDE.md Section 4.4, @docs/be/API.md Section 1.1
 */

(function() {
    'use strict';

    // DOM Elements
    let form;
    let emailInput;
    let passwordInput;
    let submitButton;
    let emailError;
    let passwordError;

    /**
     * 초기화
     */
    function init() {
        // 이미 로그인되어 있으면 리다이렉트
        if (isAuthenticated()) {
            window.location.href = '/pages/board/list.html';
            return;
        }

        cacheElements();
        bindEvents();
    }

    /**
     * DOM 요소 캐싱
     */
    function cacheElements() {
        form = document.querySelector('[data-form="login"]');
        emailInput = document.querySelector('[data-field="email"]');
        passwordInput = document.querySelector('[data-field="password"]');
        submitButton = document.querySelector('[data-action="login"]');
        emailError = document.querySelector('[data-error="email"]');
        passwordError = document.querySelector('[data-error="password"]');
    }

    /**
     * 이벤트 바인딩
     */
    function bindEvents() {
        form.addEventListener('submit', handleSubmit);

        // 입력 시 에러 메시지 제거
        emailInput.addEventListener('input', () => clearError('email'));
        passwordInput.addEventListener('input', () => clearError('password'));
    }

    /**
     * 폼 제출 핸들러
     * POST /auth/login
     */
    async function handleSubmit(event) {
        event.preventDefault();

        // 폼 검증
        if (!validateForm()) {
            return;
        }

        const email = emailInput.value.trim();
        const password = passwordInput.value;

        try {
            setLoading(true);

            // API 호출
            const response = await fetchWithAuth('/auth/login', {
                method: 'POST',
                body: JSON.stringify({ email, password })
            });

            // 토큰 저장
            localStorage.setItem('access_token', response.accessToken);
            localStorage.setItem('refresh_token', response.refreshToken);

            // 성공 메시지 (선택)
            // showSuccess('로그인되었습니다.');

            // 게시글 목록으로 리다이렉트
            window.location.href = '/pages/board/list.html';

        } catch (error) {
            handleLoginError(error);
        } finally {
            setLoading(false);
        }
    }

    /**
     * 폼 유효성 검증
     * @returns {boolean}
     */
    function validateForm() {
        let isValid = true;

        const email = emailInput.value.trim();
        const password = passwordInput.value;

        // 이메일 검증
        if (!email) {
            showError('email', '이메일을 입력해주세요.');
            isValid = false;
        } else if (!isValidEmail(email)) {
            showError('email', '올바른 이메일 형식이 아닙니다.');
            isValid = false;
        }

        // 비밀번호 검증 (빈 값만 체크)
        if (!password) {
            showError('password', '비밀번호를 입력해주세요.');
            isValid = false;
        }

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
        const errorElement = field === 'email' ? emailError : passwordError;
        const inputElement = field === 'email' ? emailInput : passwordInput;

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
        const errorElement = field === 'email' ? emailError : passwordError;
        const inputElement = field === 'email' ? emailInput : passwordInput;

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
            submitButton.disabled = true;
            submitButton.textContent = '로그인 중...';
        } else {
            submitButton.disabled = false;
            submitButton.textContent = '로그인';
        }
    }

    // 초기화
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
