/**
 * Board Edit Page Script
 * 게시글 수정 페이지 로직
 * 참조: @docs/be/API.md Section 3.4
 */

(function(window, document) {
  'use strict';

  const CONFIG = {
    MAX_TITLE_LENGTH: 27,
    MAX_FILE_SIZE: 5 * 1024 * 1024,
    ALLOWED_FILE_TYPES: ['image/jpeg', 'image/png', 'image/gif'],
    DETAIL_URL: '/pages/board/detail.html',
    LOGIN_URL: '/pages/user/login.html'
  };

  const state = {
    postId: null,
    originalPost: null,
    uploadedImageId: null,
    selectedFile: null,
    removeExistingImage: false,
    isSubmitting: false,
    hasChanges: false,  // 변경사항 추적
    initialData: {      // 초기 데이터 저장
      title: '',
      content: '',
      imageUrl: ''
    }
  };

  const elements = {
    form: null,
    titleInput: null,
    contentTextarea: null,
    imageInput: null,
    previewImage: null,
    placeholder: null,
    removeImageButton: null,
    submitButton: null,
    profileDropdown: null,
    profileImage: null,
    titleError: null,
    contentError: null,
    imageError: null
  };

  function init() {
    const urlParams = new URLSearchParams(window.location.search);
    state.postId = urlParams.get('id');

    // postId 검증 (빠른 새로고침 대비)
    if (!state.postId) {
      Toast.error('게시글을 찾을 수 없습니다.', '오류', 2000, () => {
        window.history.back();
      });
      return;
    }

    cacheElements();
    cleanupInlineStyles();  // 인라인 스타일 정리
    bindEvents();
    loadUserProfile();  // 프로필 로드
    setupBackNavigation();  // 브라우저 뒤로가기 처리
    loadPost();
  }

  function cacheElements() {
    elements.form = document.querySelector('[data-form="edit-post"]');
    elements.titleInput = document.querySelector('[data-field="title"]');
    elements.contentTextarea = document.querySelector('[data-field="content"]');
    elements.imageInput = document.querySelector('[data-field="image"]');
    elements.previewImage = document.querySelector('[data-image="preview"]');
    elements.placeholder = document.querySelector('[data-placeholder="image"]');
    elements.removeImageButton = document.querySelector('[data-action="remove-image"]');
    elements.submitButton = document.querySelector('[data-action="submit"]');
    elements.profileDropdown = document.querySelector('[data-dropdown="profile"]');
    elements.profileImage = document.querySelector('[data-profile="image"]');
    elements.titleError = document.querySelector('[data-error="title"]');
    elements.contentError = document.querySelector('[data-error="content"]');
    elements.imageError = document.querySelector('[data-error="image"]');
  }

  /**
   * 오류 요소의 인라인 스타일 제거 (CSS가 제어하도록)
   * 이전 실행으로 남아있을 수 있는 display 스타일 정리
   */
  function cleanupInlineStyles() {
    const errorElements = [elements.titleError, elements.contentError, elements.imageError];
    errorElements.forEach(el => {
      if (el) {
        el.style.display = '';  // 인라인 스타일 제거
        el.textContent = '';     // 텍스트 초기화
      }
    });
  }

  function bindEvents() {
    if (elements.form) {
      elements.form.addEventListener('submit', handleSubmit);
    }
    if (elements.imageInput) {
      elements.imageInput.addEventListener('change', handleImageChange);
    }
    if (elements.removeImageButton) {
      elements.removeImageButton.addEventListener('click', handleRemoveImage);
    }
    if (elements.profileDropdown) {
      elements.profileDropdown.addEventListener('click', handleProfileMenuClick);
    }

    // 뒤로가기/취소 버튼
    document.querySelector('[data-action="go-back"]')?.addEventListener('click', handleBack);
    document.querySelector('[data-action="cancel"]')?.addEventListener('click', handleCancel);

    // 입력 시 변경 감지
    if (elements.titleInput) {
      elements.titleInput.addEventListener('input', () => {
        checkForChanges();
      });
    }
    if (elements.contentTextarea) {
      elements.contentTextarea.addEventListener('input', () => {
        checkForChanges();
      });
    }
  }

  // ============================================
  // Profile Load
  // ============================================
  async function loadUserProfile() {
    const profileMenu = document.querySelector('[data-auth="authenticated"]');

    if (!isAuthenticated()) {
      // 비로그인: 프로필 메뉴 숨김
      if (profileMenu) profileMenu.style.display = 'none';
      return;
    }

    // 로그인: 프로필 메뉴 표시
    if (profileMenu) profileMenu.style.display = 'flex';

    try {
      const userId = getCurrentUserId();

      // userId 검증
      if (!userId) {
        console.warn('Invalid userId, skipping profile load');
        return;
      }

      const user = await fetchWithAuth(`/users/${userId}`);

      if (user) {
        // 프로필 이미지 업데이트
        const profileImage = document.querySelector('[data-profile="image"]');
        const profileName = document.querySelector('[data-profile="nickname"]');

        if (profileImage && user.profileImage) {
          profileImage.src = user.profileImage;
        }

        if (profileName && user.nickname) {
          profileName.textContent = user.nickname;
        }
      }
    } catch (error) {
      console.error('Failed to load user profile:', error);
    }
  }

  function handleProfileMenuClick(e) {
    const logoutTarget = e.target.closest('[data-action="logout"]');
    if (logoutTarget) {
      e.preventDefault();
      handleLogout();
      return;
    }

    const profileLink = e.target.closest('[data-action="profile-link"], a.profile-menu__item');
    if (profileLink && profileLink.tagName === 'A') {
      e.preventDefault();
      const href = profileLink.getAttribute('href');
      if (href) {
        window.location.href = href;
      }
    }
  }

  async function loadPost() {
    try {
      const post = await fetchWithAuth(`/posts/${state.postId}`);
      state.originalPost = post;

      // 폼 채우기
      elements.titleInput.value = post.title;
      elements.contentTextarea.value = post.content;

      // 초기 데이터 저장 (변경 감지용)
      state.initialData.title = post.title;
      state.initialData.content = post.content;

      // 기존 이미지 표시
      if (post.images && post.images.length > 0) {
        const imageUrl = post.images[0];
        elements.previewImage.src = imageUrl;
        elements.previewImage.style.display = 'block';
        elements.placeholder.style.display = 'none';
        elements.removeImageButton.style.display = 'block';
        state.initialData.imageUrl = imageUrl;
        // imageId는 API 응답에 포함되어야 함 (백엔드 확인 필요)
        if (post.imageId) {
          state.uploadedImageId = post.imageId;
        }
      }

      // 초기 상태에서는 저장 버튼 비활성화
      updateSubmitButtonState();
    } catch (error) {
      console.error('Failed to load post:', error);
      Toast.error('게시글을 불러오는데 실패했습니다.', '오류', 2000, () => {
        window.history.back();
      });
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (state.isSubmitting) return;

    if (!validateForm()) return;

    state.isSubmitting = true;
    elements.submitButton.disabled = true;
    elements.submitButton.innerHTML = '<span class="loading-spinner"></span> 수정 중...';

    try {
      // 새 이미지 업로드
      if (state.selectedFile) {
        try {
          const imageResult = await uploadImage(state.selectedFile);
          state.uploadedImageId = imageResult.imageId;  // camelCase 수정
        } catch (error) {
          console.error('Image upload failed:', error);
          const translatedMessage = translateErrorCode(error.message);
          Toast.error(translatedMessage || '이미지 업로드에 실패했습니다.', '오류');
          state.isSubmitting = false;
          elements.submitButton.disabled = false;
          elements.submitButton.textContent = '수정완료';
          return;
        }
      }

      // 게시글 수정 (PATCH)
      const body = {
        title: elements.titleInput.value.trim(),
        content: elements.contentTextarea.value.trim()
      };

      // 이미지 처리 (API.md Section 3.4 스펙 준수)
      if (state.removeExistingImage) {
        body.removeImage = true;  // 명시적 제거 신호
        console.log('[DEBUG] 이미지 제거 요청:', { removeImage: true });
      } else if (state.uploadedImageId) {
        body.imageId = state.uploadedImageId;
        console.log('[DEBUG] 이미지 변경 요청:', { imageId: state.uploadedImageId });
      }

      console.log('[DEBUG] 게시글 수정 요청 body:', JSON.stringify(body, null, 2));

      await fetchWithAuth(`/posts/${state.postId}`, {
        method: 'PATCH',
        body: JSON.stringify(body)
      });

      // 성공 - 토스트 메시지 후 상세 페이지로 이동
      state.hasChanges = false;  // beforeunload 경고 건너뛰기
      Toast.success('게시글이 수정되었습니다.', '수정 완료', 1200, () => {
        window.location.replace(`${CONFIG.DETAIL_URL}?id=${state.postId}`);  // replace()로 history 중복 방지
      });
    } catch (error) {
      console.error('Failed to update post:', error);
      const errorMessage = translateErrorCode(error.message) || '게시글 수정에 실패했습니다.';
      Toast.error(errorMessage, '오류');
      state.isSubmitting = false;
      elements.submitButton.disabled = false;
      elements.submitButton.textContent = '수정완료';  // 스피너 제거
    }
  }

  function handleImageChange(e) {
    const file = e.target.files[0];
    if (!file) return;

    if (!validateImage(file)) {
      e.target.value = '';
      return;
    }

    state.selectedFile = file;
    state.removeExistingImage = false;

    const reader = new FileReader();
    reader.onload = e => {
      elements.previewImage.src = e.target.result;
      elements.previewImage.style.display = 'block';
      elements.placeholder.style.display = 'none';
      elements.removeImageButton.style.display = 'block';
    };
    reader.readAsDataURL(file);

    // 변경 감지
    checkForChanges();
  }

  function handleRemoveImage() {
    state.selectedFile = null;
    state.uploadedImageId = null;
    state.removeExistingImage = true;

    console.log('[DEBUG] handleRemoveImage 호출:', {
      selectedFile: state.selectedFile,
      uploadedImageId: state.uploadedImageId,
      removeExistingImage: state.removeExistingImage
    });

    elements.previewImage.src = '';
    elements.previewImage.style.display = 'none';
    elements.placeholder.style.display = 'flex';
    elements.removeImageButton.style.display = 'none';
    elements.imageInput.value = '';

    // 변경 감지
    checkForChanges();
  }

  function handleProfileMenuClick(e) {
    const logoutTarget = e.target.closest('[data-action="logout"]');
    if (logoutTarget) {
      e.preventDefault();
      handleLogout();
      return;
    }

    const profileLink = e.target.closest('[data-action="profile-link"]');
    if (profileLink) {
      e.preventDefault();
      const href = profileLink.getAttribute('href');
      if (href) {
        window.location.href = href;  // Changed from replace() to preserve history
      }
    }
  }

  async function handleLogout() {
    try {
      await logout();
    } catch (error) {
      console.error('Logout failed:', error);
    } finally {
      window.location.replace(CONFIG.LOGIN_URL);
    }
  }

  async function handleBack(e) {
    e.preventDefault();
    if (state.hasChanges) {
      const confirmed = await confirmModal(
        '수정 중인 내용 확인',
        '저장하지 않은 변경사항이 있습니다. 페이지를 나가시겠습니까?'
      );
      if (!confirmed) return;
      state.hasChanges = false;  // beforeunload 건너뛰기
    }
    window.location.replace(`${CONFIG.DETAIL_URL}?id=${state.postId}`);  // replace()로 history 중복 방지
  }

  async function handleCancel(e) {
    e.preventDefault();
    if (state.hasChanges) {
      const confirmed = await confirmModal(
        '수정 중인 내용 확인',
        '저장하지 않은 변경사항이 있습니다. 취소하시겠습니까?'
      );
      if (!confirmed) return;
      state.hasChanges = false;  // beforeunload 건너뛰기
    }
    window.location.replace(`${CONFIG.DETAIL_URL}?id=${state.postId}`);  // replace()로 history 중복 방지
  }

  function checkForChanges() {
    const currentTitle = elements.titleInput ? elements.titleInput.value : '';
    const currentContent = elements.contentTextarea ? elements.contentTextarea.value : '';
    const currentImageUrl = elements.previewImage && elements.previewImage.style.display !== 'none'
      ? elements.previewImage.src : '';

    const hasTitleChange = currentTitle !== state.initialData.title;
    const hasContentChange = currentContent !== state.initialData.content;
    const hasImageChange = state.selectedFile !== null || state.removeExistingImage ||
                          currentImageUrl !== state.initialData.imageUrl;

    state.hasChanges = hasTitleChange || hasContentChange || hasImageChange;
    updateSubmitButtonState();
  }

  function updateSubmitButtonState() {
    if (elements.submitButton) {
      elements.submitButton.disabled = !state.hasChanges || state.isSubmitting;
    }
  }

  /**
   * 브라우저 뒤로가기 처리
   */
  function setupBackNavigation() {
    // beforeunload: 페이지 떠날 때
    window.addEventListener('beforeunload', (event) => {
      // 변경사항 있으면 브라우저 기본 경고
      if (state.hasChanges) {
        event.preventDefault();
        event.returnValue = ''; // Chrome 필수
        return '';
      }
    });
  }

  function validateForm() {
    let isValid = true;
    const title = elements.titleInput.value.trim();
    const content = elements.contentTextarea.value.trim();

    if (!title) {
      elements.titleError.textContent = '제목을 입력해주세요.';
      // CSS의 :not(:empty) 선택자가 자동으로 visibility 제어
      isValid = false;
    } else if (title.length > CONFIG.MAX_TITLE_LENGTH) {
      elements.titleError.textContent = `제목은 ${CONFIG.MAX_TITLE_LENGTH}자 이하로 입력해주세요.`;
      // CSS의 :not(:empty) 선택자가 자동으로 visibility 제어
      isValid = false;
    } else {
      elements.titleError.textContent = '';
      // CSS의 :not(:empty) 선택자가 자동으로 visibility 제어
    }

    if (!content) {
      elements.contentError.textContent = '내용을 입력해주세요.';
      // CSS의 :not(:empty) 선택자가 자동으로 visibility 제어
      isValid = false;
    } else {
      elements.contentError.textContent = '';
      // CSS의 :not(:empty) 선택자가 자동으로 visibility 제어
    }

    return isValid;
  }

  function validateImage(file) {
    if (file.size > CONFIG.MAX_FILE_SIZE) {
      elements.imageError.textContent = '이미지 파일 크기는 5MB 이하여야 합니다.';
      // CSS의 :not(:empty) 선택자가 자동으로 visibility 제어
      return false;
    }
    if (!CONFIG.ALLOWED_FILE_TYPES.includes(file.type)) {
      elements.imageError.textContent = 'JPG, PNG, GIF 파일만 업로드 가능합니다.';
      // CSS의 :not(:empty) 선택자가 자동으로 visibility 제어
      return false;
    }
    elements.imageError.textContent = '';
    // CSS의 :not(:empty) 선택자가 자동으로 visibility 제어
    return true;
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

})(window, document);
