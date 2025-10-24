/**
 * Register Page Script
 * 회원가입 페이지 로직
 * 참조: @CLAUDE.md Section 4.5, @docs/be/API.md Section 2.1
 */

(function(window, document) {
    'use strict';

    // ============================================
    // Configuration
    // ============================================
    const CONFIG = {
        LIST_URL: '/pages/board/list.html',
        API_BASE_URL: 'http://localhost:8080',
        MAX_NICKNAME_LENGTH: 10,
        MAX_FILE_SIZE: 5 * 1024 * 1024,
        ALLOWED_FILE_TYPES: ['image/jpeg', 'image/png', 'image/gif']
    };

    // ============================================
    // DOM Element Caching
    // ============================================
    const elements = {
        form: null,
        profileImageInput: null,
        profileImagePreview: null,
        profileImageElement: null,
        profilePlaceholder: null,
        emailInput: null,
        passwordInput: null,
        passwordConfirmInput: null,
        nicknameInput: null,
        submitButton: null,
        goBackButton: null,
        // Error elements
        profileImageError: null,
        emailError: null,
        passwordError: null,
        passwordConfirmError: null,
        nicknameError: null
    };

    /**
     * 초기화
     */
    function init() {
        // 이미 로그인되어 있으면 리다이렉트
        if (isAuthenticated()) {
            window.location.href = CONFIG.LIST_URL;
            return;
        }

        cacheElements();
        bindEvents();
    }

    /**
     * DOM 요소 캐싱
     */
    function cacheElements() {
        elements.form = document.querySelector('[data-form="register"]');
        elements.profileImageInput = document.querySelector('[data-field="profileImage"]');
        elements.profileImagePreview = document.querySelector('[data-preview="profile"]');
        elements.profileImageElement = document.querySelector('[data-image="profile"]');
        elements.profilePlaceholder = document.querySelector('[data-placeholder="profile"]');
        elements.emailInput = document.querySelector('[data-field="email"]');
        elements.passwordInput = document.querySelector('[data-field="password"]');
        elements.passwordConfirmInput = document.querySelector('[data-field="passwordConfirm"]');
        elements.nicknameInput = document.querySelector('[data-field="nickname"]');
        elements.submitButton = document.querySelector('[data-action="register"]');
        elements.goBackButton = document.querySelector('[data-action="go-back"]');

        // Error elements
        elements.profileImageError = document.querySelector('[data-error="profileImage"]');
        elements.emailError = document.querySelector('[data-error="email"]');
        elements.passwordError = document.querySelector('[data-error="password"]');
        elements.passwordConfirmError = document.querySelector('[data-error="passwordConfirm"]');
        elements.nicknameError = document.querySelector('[data-error="nickname"]');
    }

    /**
     * 이벤트 바인딩
     */
    function bindEvents() {
        elements.form.addEventListener('submit', handleSubmit);
        elements.profileImageInput.addEventListener('change', handleImageChange);
        elements.goBackButton.addEventListener('click', () => window.history.back());

        // 입력 시 에러 메시지 제거
        elements.profileImageInput.addEventListener('change', () => clearError('profileImage'));
        elements.emailInput.addEventListener('input', () => clearError('email'));
        elements.passwordInput.addEventListener('input', () => clearError('password'));
        elements.passwordConfirmInput.addEventListener('input', () => clearError('passwordConfirm'));
        elements.nicknameInput.addEventListener('input', () => clearError('nickname'));
    }

    /**
     * 프로필 이미지 변경 핸들러
     */
    function handleImageChange(event) {
        const file = event.target.files[0];
        if (!file) return;

        // 파일 검증
        const error = getImageFileError(file);
        if (error) {
            showError('profileImage', error);
            event.target.value = '';
            return;
        }

        // 미리보기 표시
        const reader = new FileReader();
        reader.onload = (e) => {
            elements.profileImageElement.src = e.target.result;
            elements.profileImageElement.style.display = 'block';
            elements.profilePlaceholder.style.display = 'none';
        };
        reader.readAsDataURL(file);
    }

    /**
     * 폼 제출 핸들러
     * POST /users/signup (multipart/form-data)
     */
    async function handleSubmit(event) {
        event.preventDefault();

        // 폼 검증
        if (!validateForm()) {
            return;
        }

        const email = elements.emailInput.value.trim();
        const password = elements.passwordInput.value;
        const nickname = elements.nicknameInput.value.trim();
        const profileImage = elements.profileImageInput.files[0];

        // FormData 구성
        const formData = new FormData();
        formData.append('email', email);
        formData.append('password', password);
        formData.append('nickname', nickname);

        if (profileImage) {
            formData.append('profileImage', profileImage);  // camelCase로 통일
        }

        try {
            setLoading(true);

            // API 호출 (multipart/form-data이므로 fetch 직접 사용)
            const response = await fetch(`${CONFIG.API_BASE_URL}/users/signup`, {
                method: 'POST',
                body: formData // Content-Type 자동 설정
            });

            const data = await response.json();

            if (response.ok) {
                // 자동 로그인 (토큰 저장)
                localStorage.setItem('access_token', data.data.accessToken);
                localStorage.setItem('refresh_token', data.data.refreshToken);

                // 게시글 목록으로 리다이렉트
                window.location.href = CONFIG.LIST_URL;
            } else {
                throw new Error(data.message);
            }

        } catch (error) {
            handleRegisterError(error);
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

        const email = elements.emailInput.value.trim();
        const password = elements.passwordInput.value;
        const passwordConfirm = elements.passwordConfirmInput.value;
        const nickname = elements.nicknameInput.value.trim();

        // 이메일 검증
        if (!email) {
            showError('email', '이메일을 입력해주세요.');
            isValid = false;
        } else if (!isValidEmail(email)) {
            showError('email', '올바른 이메일 형식이 아닙니다.');
            isValid = false;
        }

        // 비밀번호 검증
        if (!password) {
            showError('password', '비밀번호를 입력해주세요.');
            isValid = false;
        } else if (!isValidPassword(password)) {
            showError('password', getPasswordPolicyMessage());
            isValid = false;
        }

        // 비밀번호 확인 검증
        if (!passwordConfirm) {
            showError('passwordConfirm', '비밀번호 확인을 입력해주세요.');
            isValid = false;
        } else if (!isPasswordMatch(password, passwordConfirm)) {
            showError('passwordConfirm', '비밀번호가 일치하지 않습니다.');
            isValid = false;
        }

        // 닉네임 검증
        if (!nickname) {
            showError('nickname', '닉네임을 입력해주세요.');
            isValid = false;
        } else if (!isValidNickname(nickname)) {
            showError('nickname', `닉네임은 1-${CONFIG.MAX_NICKNAME_LENGTH}자 이내로 입력해주세요.`);
            isValid = false;
        }

        return isValid;
    }

    /**
     * 회원가입 에러 처리
     * @param {Error} error
     */
    function handleRegisterError(error) {
        const message = error.message || '';
        const translatedMessage = translateErrorCode(message);

        // USER-002: 이메일 중복
        if (message.includes('USER-002')) {
            showError('email', translatedMessage);
        }
        // USER-003: 닉네임 중복
        else if (message.includes('USER-003')) {
            showError('nickname', translatedMessage);
        }
        // USER-004: 비밀번호 정책 위반
        else if (message.includes('USER-004')) {
            showError('password', translatedMessage);
        }
        // IMAGE-002: 파일 크기 초과
        else if (message.includes('IMAGE-002')) {
            showError('profileImage', translatedMessage);
        }
        // IMAGE-003: 파일 형식 오류
        else if (message.includes('IMAGE-003')) {
            showError('profileImage', translatedMessage);
        }
        // 기타 에러
        else {
            showError('email', translatedMessage || '회원가입에 실패했습니다.');
        }
    }

    /**
     * 에러 메시지 표시
     * @param {string} field
     * @param {string} message
     */
    function showError(field, message) {
        const errorMap = {
            profileImage: elements.profileImageError,
            email: elements.emailError,
            password: elements.passwordError,
            passwordConfirm: elements.passwordConfirmError,
            nickname: elements.nicknameError
        };

        const inputMap = {
            profileImage: elements.profileImageInput,
            email: elements.emailInput,
            password: elements.passwordInput,
            passwordConfirm: elements.passwordConfirmInput,
            nickname: elements.nicknameInput
        };

        const errorElement = errorMap[field];
        const inputElement = inputMap[field];

        if (errorElement) {
            errorElement.textContent = message;
            errorElement.style.display = 'block';
        }

        if (inputElement && field !== 'profileImage') {
            inputElement.classList.add('input-field__input--error');
        }
    }

    /**
     * 에러 메시지 제거
     * @param {string} field
     */
    function clearError(field) {
        const errorMap = {
            profileImage: elements.profileImageError,
            email: elements.emailError,
            password: elements.passwordError,
            passwordConfirm: elements.passwordConfirmError,
            nickname: elements.nicknameError
        };

        const inputMap = {
            profileImage: elements.profileImageInput,
            email: elements.emailInput,
            password: elements.passwordInput,
            passwordConfirm: elements.passwordConfirmInput,
            nickname: elements.nicknameInput
        };

        const errorElement = errorMap[field];
        const inputElement = inputMap[field];

        if (errorElement) {
            errorElement.textContent = '';
            errorElement.style.display = 'none';
        }

        if (inputElement && field !== 'profileImage') {
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
            elements.submitButton.textContent = '회원가입 중...';
        } else {
            elements.submitButton.disabled = false;
            elements.submitButton.textContent = '회원가입';
        }
    }

    // 초기화
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})(window, document);
