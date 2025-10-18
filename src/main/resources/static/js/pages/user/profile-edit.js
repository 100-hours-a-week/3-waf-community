/**
 * Profile Edit Page Script
 * 프로필 수정 페이지 로직
 * 참조: @docs/be/API.md Section 2.2, 2.3, 2.5
 */

(function(window, document) {
  'use strict';

  const CONFIG = {
    MAX_NICKNAME_LENGTH: 10,
    MAX_FILE_SIZE: 5 * 1024 * 1024,
    ALLOWED_FILE_TYPES: ['image/jpeg', 'image/png', 'image/gif'],
    LIST_URL: '/pages/board/list.html'
  };

  const state = {
    userId: null,
    selectedFile: null
  };

  const elements = {
    form: null,
    profileImage: null,
    profileImageInput: null,
    nicknameInput: null,
    nicknameError: null,
    saveButton: null,
    withdrawButton: null
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
    loadUserData();
  }

  function cacheElements() {
    elements.form = document.querySelector('[data-form="profile-edit"]');
    elements.profileImage = document.querySelector('[data-image="profile"]');
    elements.profileImageInput = document.querySelector('[data-field="profileImage"]');
    elements.nicknameInput = document.querySelector('[data-field="nickname"]');
    elements.nicknameError = document.querySelector('[data-error="nickname"]');
    elements.saveButton = document.querySelector('[data-action="save"]');
    elements.withdrawButton = document.querySelector('[data-action="withdraw"]');
  }

  function bindEvents() {
    if (elements.form) {
      elements.form.addEventListener('submit', handleSubmit);
    }
    if (elements.profileImageInput) {
      elements.profileImageInput.addEventListener('change', handleImageChange);
    }
    if (elements.withdrawButton) {
      elements.withdrawButton.addEventListener('click', handleWithdraw);
    }
    document.querySelector('[data-action="go-back"]')?.addEventListener('click', e => {
      e.preventDefault();
      window.history.back();
    });
  }

  async function loadUserData() {
    try {
      const user = await fetchWithAuth(`/users/${state.userId}`);
      elements.nicknameInput.value = user.nickname;
      if (user.profileImage) {
        elements.profileImage.src = user.profileImage;
        elements.profileImage.style.display = 'block';
      }
    } catch (error) {
      console.error('Failed to load user data:', error);
      alert('사용자 정보를 불러오는데 실패했습니다.');
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();

    const nickname = elements.nicknameInput.value.trim();

    if (!nickname) {
      elements.nicknameError.textContent = '닉네임을 입력해주세요.';
      elements.nicknameError.style.display = 'block';
      return;
    }

    if (nickname.length > CONFIG.MAX_NICKNAME_LENGTH) {
      elements.nicknameError.textContent = `닉네임은 ${CONFIG.MAX_NICKNAME_LENGTH}자 이하로 입력해주세요.`;
      elements.nicknameError.style.display = 'block';
      return;
    }

    elements.saveButton.disabled = true;
    elements.saveButton.textContent = '저장 중...';

    try {
      const formData = new FormData();
      formData.append('nickname', nickname);
      if (state.selectedFile) {
        formData.append('profileImage', state.selectedFile);  // camelCase로 통일
      }

      const token = localStorage.getItem('access_token');
      const response = await fetch(`${API_BASE_URL}/users/${state.userId}`, {
        method: 'PATCH',
        headers: {
          Authorization: `Bearer ${token}`
        },
        body: formData
      });

      const data = await response.json();

      if (response.ok) {
        alert('프로필이 수정되었습니다.');
        window.location.reload();
      } else {
        throw new Error(data.message);
      }
    } catch (error) {
      console.error('Failed to update profile:', error);
      alert(translateErrorCode(error.message) || '프로필 수정에 실패했습니다.');
      elements.saveButton.disabled = false;
      elements.saveButton.textContent = '저장';
    }
  }

  function handleImageChange(e) {
    const file = e.target.files[0];
    if (!file) return;

    if (file.size > CONFIG.MAX_FILE_SIZE) {
      alert('이미지 파일 크기는 5MB 이하여야 합니다.');
      e.target.value = '';
      return;
    }

    if (!CONFIG.ALLOWED_FILE_TYPES.includes(file.type)) {
      alert('JPG, PNG, GIF 파일만 업로드 가능합니다.');
      e.target.value = '';
      return;
    }

    state.selectedFile = file;

    const reader = new FileReader();
    reader.onload = e => {
      elements.profileImage.src = e.target.result;
      elements.profileImage.style.display = 'block';
    };
    reader.readAsDataURL(file);
  }

  async function handleWithdraw() {
    if (!confirm('정말 탈퇴하시겠습니까? 모든 데이터가 삭제됩니다.')) return;
    if (!confirm('탈퇴 후에는 복구할 수 없습니다. 계속하시겠습니까?')) return;

    try {
      await fetchWithAuth(`/users/${state.userId}`, {
        method: 'PUT'
      });

      alert('회원탈퇴가 완료되었습니다.');
      logout();
      window.location.href = '/pages/user/login.html';
    } catch (error) {
      console.error('Failed to withdraw:', error);
      alert('회원탈퇴에 실패했습니다.');
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

})(window, document);
