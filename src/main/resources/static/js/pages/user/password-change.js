/**
 * Password Change Page Script
 * 비밀번호 변경 페이지 로직
 * 참조: @docs/be/API.md Section 2.4
 */

(function(window, document) {
  'use strict';

  const CONFIG = {
    LIST_URL: '/pages/board/list.html'
  };

  const state = {
    userId: null
  };

  const elements = {
    form: null,
    currentPasswordInput: null,
    newPasswordInput: null,
    newPasswordConfirmInput: null,
    currentPasswordError: null,
    newPasswordError: null,
    newPasswordConfirmError: null,
    saveButton: null
  };

  function init() {
    state.userId = getCurrentUserId();
    if (!state.userId) {
      alert('로그인이 필요합니다.');
      window.location.href = '/pages/user/login.html';
      return;
    }

    cacheElements();
    bindEvents();
  }

  function cacheElements() {
    elements.form = document.querySelector('[data-form="password-change"]');
    elements.currentPasswordInput = document.querySelector('[data-field="currentPassword"]');
    elements.newPasswordInput = document.querySelector('[data-field="newPassword"]');
    elements.newPasswordConfirmInput = document.querySelector('[data-field="newPasswordConfirm"]');
    elements.currentPasswordError = document.querySelector('[data-error="currentPassword"]');
    elements.newPasswordError = document.querySelector('[data-error="newPassword"]');
    elements.newPasswordConfirmError = document.querySelector('[data-error="newPasswordConfirm"]');
    elements.saveButton = document.querySelector('[data-action="save"]');
  }

  function bindEvents() {
    if (elements.form) {
      elements.form.addEventListener('submit', handleSubmit);
    }
    document.querySelector('[data-action="go-back"]')?.addEventListener('click', e => {
      e.preventDefault();
      window.history.back();
    });
  }

  async function handleSubmit(e) {
    e.preventDefault();

    const currentPassword = elements.currentPasswordInput.value;
    const newPassword = elements.newPasswordInput.value;
    const newPasswordConfirm = elements.newPasswordConfirmInput.value;

    // 검증
    if (!validateForm(currentPassword, newPassword, newPasswordConfirm)) {
      return;
    }

    elements.saveButton.disabled = true;
    elements.saveButton.textContent = '변경 중...';

    try {
      await fetchWithAuth(`/users/${state.userId}/password`, {
        method: 'PATCH',
        body: JSON.stringify({
          newPassword: newPassword,
          newPasswordConfirm: newPasswordConfirm
        })
      });

      alert('비밀번호가 변경되었습니다. 다시 로그인해주세요.');
      logout();
      window.location.href = '/pages/user/login.html';
    } catch (error) {
      console.error('Failed to change password:', error);

      // 에러 코드별 처리
      const errorMessage = error.message;
      if (errorMessage.includes('USER-006')) {
        showCurrentPasswordError('현재 비밀번호가 일치하지 않습니다.');
      } else if (errorMessage.includes('USER-004')) {
        showNewPasswordError('비밀번호 정책을 만족하지 않습니다.');
      } else {
        alert(translateErrorCode(errorMessage) || '비밀번호 변경에 실패했습니다.');
      }

      elements.saveButton.disabled = false;
      elements.saveButton.textContent = '변경';
    }
  }

  function validateForm(currentPassword, newPassword, newPasswordConfirm) {
    let isValid = true;

    // 현재 비밀번호 확인
    if (!currentPassword) {
      showCurrentPasswordError('현재 비밀번호를 입력해주세요.');
      isValid = false;
    } else {
      clearCurrentPasswordError();
    }

    // 새 비밀번호 정책 확인
    if (!newPassword) {
      showNewPasswordError('새 비밀번호를 입력해주세요.');
      isValid = false;
    } else if (!isValidPassword(newPassword)) {
      showNewPasswordError('8-20자, 영문 대/소문자, 숫자, 특수문자를 포함해야 합니다.');
      isValid = false;
    } else {
      clearNewPasswordError();
    }

    // 새 비밀번호 확인 일치 확인
    if (!newPasswordConfirm) {
      showNewPasswordConfirmError('새 비밀번호 확인을 입력해주세요.');
      isValid = false;
    } else if (newPassword !== newPasswordConfirm) {
      showNewPasswordConfirmError('새 비밀번호가 일치하지 않습니다.');
      isValid = false;
    } else {
      clearNewPasswordConfirmError();
    }

    return isValid;
  }

  function showCurrentPasswordError(message) {
    if (elements.currentPasswordError) {
      elements.currentPasswordError.textContent = message;
      elements.currentPasswordError.style.display = 'block';
    }
  }

  function clearCurrentPasswordError() {
    if (elements.currentPasswordError) {
      elements.currentPasswordError.textContent = '';
      elements.currentPasswordError.style.display = 'none';
    }
  }

  function showNewPasswordError(message) {
    if (elements.newPasswordError) {
      elements.newPasswordError.textContent = message;
      elements.newPasswordError.style.display = 'block';
    }
  }

  function clearNewPasswordError() {
    if (elements.newPasswordError) {
      elements.newPasswordError.textContent = '';
      elements.newPasswordError.style.display = 'none';
    }
  }

  function showNewPasswordConfirmError(message) {
    if (elements.newPasswordConfirmError) {
      elements.newPasswordConfirmError.textContent = message;
      elements.newPasswordConfirmError.style.display = 'block';
    }
  }

  function clearNewPasswordConfirmError() {
    if (elements.newPasswordConfirmError) {
      elements.newPasswordConfirmError.textContent = '';
      elements.newPasswordConfirmError.style.display = 'none';
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

})(window, document);
