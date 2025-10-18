/**
 * Register Page Script
 * 파일: scripts/pages/user/register.js
 * 설명: 회원가입 페이지 로직 (주석으로만 구현)
 */

(function(window, document) {
  'use strict';

  // ============================================
  // Configuration
  // ============================================
  // const CONFIG = {
  //   API_REGISTER: '/api/auth/register',
  //   API_CHECK_EMAIL: '/api/auth/check-email',
  //   API_CHECK_NICKNAME: '/api/auth/check-nickname',
  //   REDIRECT_URL: './login.html',
  //   MAX_FILE_SIZE: 5 * 1024 * 1024, // 5MB
  //   ALLOWED_FILE_TYPES: ['image/jpeg', 'image/png', 'image/gif']
  // };


  // ============================================
  // State Management
  // ============================================
  // const state = {
  //   isLoading: false,
  //   profileImage: null,
  //   validation: {
  //     email: false,
  //     password: false,
  //     passwordConfirm: false,
  //     nickname: false
  //   }
  // };


  // ============================================
  // DOM Element Caching
  // ============================================
  // const elements = {
  //   form: null,
  //   profileImageInput: null,
  //   profileImagePreview: null,
  //   profileImagePlaceholder: null,
  //   emailInput: null,
  //   passwordInput: null,
  //   passwordConfirmInput: null,
  //   nicknameInput: null,
  //   submitButton: null,
  //   passwordStrengthIndicator: null,
  //   passwordStrengthFill: null,
  //   passwordStrengthText: null,
  //   backButton: null
  // };


  // ============================================
  // Initialization
  // ============================================
  // function init() {
  //   cacheElements();
  //   bindEvents();
  //   checkAuthStatus(); // 이미 로그인되어 있으면 리다이렉트
  // }

  // function cacheElements() {
  //   elements.form = document.querySelector('[data-form="register"]');
  //   elements.profileImageInput = document.querySelector('[data-field="profileImage"]');
  //   elements.profileImagePreview = document.querySelector('[data-image="profile"]');
  //   elements.profileImagePlaceholder = document.querySelector('[data-placeholder="profile"]');
  //   elements.emailInput = document.querySelector('[data-field="email"]');
  //   elements.passwordInput = document.querySelector('[data-field="password"]');
  //   elements.passwordConfirmInput = document.querySelector('[data-field="passwordConfirm"]');
  //   elements.nicknameInput = document.querySelector('[data-field="nickname"]');
  //   elements.submitButton = document.querySelector('[data-action="register"]');
  //   elements.passwordStrengthIndicator = document.querySelector('[data-strength="indicator"]');
  //   elements.passwordStrengthFill = document.querySelector('[data-strength="fill"]');
  //   elements.passwordStrengthText = document.querySelector('[data-strength="text"]');
  //   elements.backButton = document.querySelector('[data-action="go-back"]');
  // }


  // ============================================
  // Event Binding
  // ============================================
  // function bindEvents() {
  //   // 폼 제출 이벤트
  //   elements.form.addEventListener('submit', handleSubmit);
  //
  //   // 프로필 이미지 업로드
  //   elements.profileImageInput.addEventListener('change', handleProfileImageChange);
  //
  //   // 이메일 검증 (blur 시 중복 체크)
  //   elements.emailInput.addEventListener('blur', handleEmailBlur);
  //   elements.emailInput.addEventListener('input', () => clearError('email'));
  //
  //   // 비밀번호 검증 (실시간 강도 표시)
  //   elements.passwordInput.addEventListener('input', handlePasswordInput);
  //   elements.passwordInput.addEventListener('blur', handlePasswordBlur);
  //
  //   // 비밀번호 확인 검증
  //   elements.passwordConfirmInput.addEventListener('input', handlePasswordConfirmInput);
  //   elements.passwordConfirmInput.addEventListener('blur', handlePasswordConfirmBlur);
  //
  //   // 닉네임 검증 (blur 시 중복 체크)
  //   elements.nicknameInput.addEventListener('blur', handleNicknameBlur);
  //   elements.nicknameInput.addEventListener('input', () => clearError('nickname'));
  //
  //   // 뒤로가기 버튼
  //   elements.backButton.addEventListener('click', handleBackClick);
  // }


  // ============================================
  // Event Handlers
  // ============================================
  // function handleSubmit(e) {
  //   e.preventDefault();
  //
  //   // 폼 유효성 검증
  //   if (!validateForm()) {
  //     Utils.toast.error('입력 항목을 확인해주세요');
  //     return;
  //   }
  //
  //   // 회원가입 API 호출
  //   performRegister();
  // }

  // function handleProfileImageChange(e) {
  //   const file = e.target.files[0];
  //
  //   if (!file) {
  //     return;
  //   }
  //
  //   // 파일 유효성 검증
  //   const validation = Validation.file(file, {
  //     maxSize: CONFIG.MAX_FILE_SIZE,
  //     allowedTypes: CONFIG.ALLOWED_FILE_TYPES
  //   });
  //
  //   if (!validation.valid) {
  //     showError('profileImage', validation.message);
  //     e.target.value = '';
  //     return;
  //   }
  //
  //   // 미리보기 표시
  //   const reader = new FileReader();
  //   reader.onload = (event) => {
  //     elements.profileImagePreview.src = event.target.result;
  //     elements.profileImagePreview.style.display = 'block';
  //     elements.profileImagePlaceholder.style.display = 'none';
  //     state.profileImage = file;
  //     clearError('profileImage');
  //   };
  //   reader.readAsDataURL(file);
  // }

  // function handleEmailBlur(e) {
  //   const email = e.target.value.trim();
  //
  //   // 이메일 형식 검증
  //   const result = Validation.email(email);
  //   if (!result.valid) {
  //     showError('email', result.message);
  //     state.validation.email = false;
  //     return;
  //   }
  //
  //   // 이메일 중복 확인
  //   checkEmailDuplicate(email);
  // }

  // function handlePasswordInput(e) {
  //   const password = e.target.value;
  //
  //   // 비밀번호 강도 표시
  //   updatePasswordStrength(password);
  //
  //   // 비밀번호 확인 필드가 있으면 일치 여부 확인
  //   if (elements.passwordConfirmInput.value) {
  //     handlePasswordConfirmInput();
  //   }
  // }

  // function handlePasswordBlur(e) {
  //   const password = e.target.value;
  //   const result = Validation.password(password);
  //
  //   if (!result.valid) {
  //     showError('password', result.message);
  //     state.validation.password = false;
  //   } else {
  //     clearError('password');
  //     state.validation.password = true;
  //   }
  // }

  // function handlePasswordConfirmInput(e) {
  //   const password = elements.passwordInput.value;
  //   const passwordConfirm = elements.passwordConfirmInput.value;
  //
  //   clearError('passwordConfirm');
  //
  //   if (passwordConfirm && password !== passwordConfirm) {
  //     showError('passwordConfirm', '비밀번호가 일치하지 않습니다');
  //     state.validation.passwordConfirm = false;
  //   } else if (passwordConfirm && password === passwordConfirm) {
  //     showSuccess('passwordConfirm', '비밀번호가 일치합니다');
  //     state.validation.passwordConfirm = true;
  //   }
  // }

  // function handlePasswordConfirmBlur(e) {
  //   handlePasswordConfirmInput(e);
  // }

  // function handleNicknameBlur(e) {
  //   const nickname = e.target.value.trim();
  //
  //   // 닉네임 길이 검증
  //   const result = Validation.nickname(nickname);
  //   if (!result.valid) {
  //     showError('nickname', result.message);
  //     state.validation.nickname = false;
  //     return;
  //   }
  //
  //   // 닉네임 중복 확인
  //   checkNicknameDuplicate(nickname);
  // }

  // function handleBackClick(e) {
  //   e.preventDefault();
  //   window.history.back();
  // }


  // ============================================
  // Validation Functions
  // ============================================
  // function validateForm() {
  //   let isValid = true;
  //
  //   // 이메일 검증
  //   if (!state.validation.email) {
  //     showError('email', '이메일을 확인해주세요');
  //     isValid = false;
  //   }
  //
  //   // 비밀번호 검증
  //   if (!state.validation.password) {
  //     showError('password', '비밀번호를 확인해주세요');
  //     isValid = false;
  //   }
  //
  //   // 비밀번호 확인 검증
  //   if (!state.validation.passwordConfirm) {
  //     showError('passwordConfirm', '비밀번호 확인을 입력해주세요');
  //     isValid = false;
  //   }
  //
  //   // 닉네임 검증
  //   if (!state.validation.nickname) {
  //     showError('nickname', '닉네임을 확인해주세요');
  //     isValid = false;
  //   }
  //
  //   return isValid;
  // }

  // function updatePasswordStrength(password) {
  //   if (!password) {
  //     elements.passwordStrengthIndicator.style.display = 'none';
  //     return;
  //   }
  //
  //   elements.passwordStrengthIndicator.style.display = 'block';
  //
  //   // 비밀번호 강도 계산
  //   let strength = 0;
  //   if (password.length >= 8) strength++;
  //   if (/[a-z]/.test(password)) strength++;
  //   if (/[A-Z]/.test(password)) strength++;
  //   if (/[0-9]/.test(password)) strength++;
  //   if (/[^a-zA-Z0-9]/.test(password)) strength++;
  //
  //   // 강도 표시
  //   elements.passwordStrengthFill.className = 'password-strength__fill';
  //   if (strength <= 2) {
  //     elements.passwordStrengthFill.classList.add('password-strength__fill--weak');
  //     elements.passwordStrengthText.textContent = '약함';
  //     elements.passwordStrengthText.style.color = 'var(--color-error)';
  //   } else if (strength <= 4) {
  //     elements.passwordStrengthFill.classList.add('password-strength__fill--medium');
  //     elements.passwordStrengthText.textContent = '보통';
  //     elements.passwordStrengthText.style.color = 'var(--color-warning)';
  //   } else {
  //     elements.passwordStrengthFill.classList.add('password-strength__fill--strong');
  //     elements.passwordStrengthText.textContent = '강함';
  //     elements.passwordStrengthText.style.color = 'var(--color-success)';
  //   }
  // }


  // ============================================
  // API Functions
  // ============================================
  // async function checkEmailDuplicate(email) {
  //   try {
  //     const response = await API.get(`${CONFIG.API_CHECK_EMAIL}?email=${encodeURIComponent(email)}`);
  //
  //     if (response.available) {
  //       showSuccess('email', '사용 가능한 이메일입니다');
  //       state.validation.email = true;
  //     } else {
  //       showError('email', '이미 사용 중인 이메일입니다');
  //       state.validation.email = false;
  //     }
  //   } catch (error) {
  //     showError('email', '이메일 확인 중 오류가 발생했습니다');
  //     state.validation.email = false;
  //   }
  // }

  // async function checkNicknameDuplicate(nickname) {
  //   try {
  //     const response = await API.get(`${CONFIG.API_CHECK_NICKNAME}?nickname=${encodeURIComponent(nickname)}`);
  //
  //     if (response.available) {
  //       showSuccess('nickname', '사용 가능한 닉네임입니다');
  //       state.validation.nickname = true;
  //     } else {
  //       showError('nickname', '이미 사용 중인 닉네임입니다');
  //       state.validation.nickname = false;
  //     }
  //   } catch (error) {
  //     showError('nickname', '닉네임 확인 중 오류가 발생했습니다');
  //     state.validation.nickname = false;
  //   }
  // }

  // async function performRegister() {
  //   try {
  //     // 로딩 상태 시작
  //     setLoading(true);
  //
  //     // FormData 생성 (프로필 이미지 포함)
  //     const formData = new FormData();
  //     if (state.profileImage) {
  //       formData.append('profileImage', state.profileImage);
  //     }
  //     formData.append('email', elements.emailInput.value.trim());
  //     formData.append('password', elements.passwordInput.value);
  //     formData.append('nickname', elements.nicknameInput.value.trim());
  //
  //     // API 호출
  //     const response = await API.upload(CONFIG.API_REGISTER, formData);
  //
  //     // 성공 처리
  //     handleRegisterSuccess(response.data);
  //
  //   } catch (error) {
  //     // 에러 처리
  //     handleRegisterError(error);
  //   } finally {
  //     // 로딩 상태 종료
  //     setLoading(false);
  //   }
  // }

  // function handleRegisterSuccess(data) {
  //   // 성공 메시지
  //   Utils.toast.success('회원가입이 완료되었습니다. 로그인해주세요.');
  //
  //   // 로그인 페이지로 리다이렉트
  //   setTimeout(() => {
  //     window.location.href = CONFIG.REDIRECT_URL;
  //   }, 1000);
  // }

  // function handleRegisterError(error) {
  //   const message = error.message || '회원가입에 실패했습니다. 다시 시도해주세요.';
  //   Utils.toast.error(message);
  //
  //   // 특정 필드 에러인 경우
  //   if (error.field) {
  //     showError(error.field, message);
  //   }
  // }


  // ============================================
  // UI Helper Functions
  // ============================================
  // function setLoading(loading) {
  //   state.isLoading = loading;
  //
  //   if (loading) {
  //     elements.submitButton.disabled = true;
  //     elements.submitButton.classList.add('btn--loading');
  //   } else {
  //     elements.submitButton.disabled = false;
  //     elements.submitButton.classList.remove('btn--loading');
  //   }
  // }

  // function showError(field, message) {
  //   const errorElement = document.querySelector(`[data-error="${field}"]`);
  //   const inputElement = document.querySelector(`[data-field="${field}"]`);
  //
  //   if (errorElement) {
  //     errorElement.textContent = message;
  //     errorElement.style.display = 'block';
  //   }
  //
  //   if (inputElement) {
  //     inputElement.classList.add('input-field__input--error');
  //   }
  //
  //   // Success 메시지 숨김
  //   const successElement = document.querySelector(`[data-success="${field}"]`);
  //   if (successElement) {
  //     successElement.textContent = '';
  //     successElement.style.display = 'none';
  //   }
  // }

  // function showSuccess(field, message) {
  //   const successElement = document.querySelector(`[data-success="${field}"]`);
  //   const inputElement = document.querySelector(`[data-field="${field}"]`);
  //
  //   if (successElement) {
  //     successElement.textContent = message;
  //     successElement.style.display = 'block';
  //   }
  //
  //   if (inputElement) {
  //     inputElement.classList.remove('input-field__input--error');
  //     inputElement.classList.add('input-field__input--success');
  //   }
  //
  //   // Error 메시지 숨김
  //   clearError(field);
  // }

  // function clearError(field) {
  //   const errorElement = document.querySelector(`[data-error="${field}"]`);
  //   const inputElement = document.querySelector(`[data-field="${field}"]`);
  //
  //   if (errorElement) {
  //     errorElement.textContent = '';
  //     errorElement.style.display = 'none';
  //   }
  //
  //   if (inputElement) {
  //     inputElement.classList.remove('input-field__input--error');
  //   }
  // }


  // ============================================
  // Auth Check
  // ============================================
  // function checkAuthStatus() {
  //   const token = API.getToken();
  //
  //   // 이미 로그인되어 있으면 게시글 목록으로 리다이렉트
  //   if (token) {
  //     window.location.href = '../../pages/board/list.html';
  //   }
  // }


  // ============================================
  // DOMContentLoaded Event
  // ============================================
  // if (document.readyState === 'loading') {
  //   document.addEventListener('DOMContentLoaded', init);
  // } else {
  //   init();
  // }

  // TODO: 위 주석을 해제하고 실제 구현 진행

})(window, document);
