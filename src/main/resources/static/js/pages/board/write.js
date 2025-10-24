/**
 * Board Write Page Script
 * 게시글 작성 페이지 로직
 * 참조: @CLAUDE.md Section 4.3, @docs/be/API.md Section 3.3, 4.1
 */

(function(window, document) {
  'use strict';

  // ============================================
  // Configuration
  // ============================================
  const CONFIG = {
    MAX_TITLE_LENGTH: 27,
    MAX_FILE_SIZE: 5 * 1024 * 1024, // 5MB
    ALLOWED_FILE_TYPES: ['image/jpeg', 'image/png', 'image/gif'],
    LIST_URL: '/pages/board/list.html',
    DETAIL_URL: '/pages/board/detail.html'
  };

  // ============================================
  // State Management
  // ============================================
  const state = {
    uploadedImageId: null,
    uploadedImageUrl: null,
    selectedFile: null,
    isSubmitting: false
  };

  // ============================================
  // DOM Element Caching
  // ============================================
  const elements = {
    form: null,
    titleInput: null,
    contentTextarea: null,
    imageInput: null,
    previewImage: null,
    placeholder: null,
    backButton: null,
    cancelButton: null,
    submitButton: null,

    // Error elements
    titleError: null,
    contentError: null,
    imageError: null
  };

  // ============================================
  // Initialization
  // ============================================
  function init() {
    cacheElements();
    bindEvents();
  }

  function cacheElements() {
    elements.form = document.querySelector('[data-form="write-post"]');
    elements.titleInput = document.querySelector('[data-field="title"]');
    elements.contentTextarea = document.querySelector('[data-field="content"]');
    elements.imageInput = document.querySelector('[data-field="image"]');
    elements.previewImage = document.querySelector('[data-image="preview"]');
    elements.placeholder = document.querySelector('[data-placeholder="image"]');
    elements.backButton = document.querySelector('[data-action="go-back"]');
    elements.cancelButton = document.querySelector('[data-action="cancel"]');
    elements.submitButton = document.querySelector('[data-action="submit"]');

    // Error elements
    elements.titleError = document.querySelector('[data-error="title"]');
    elements.contentError = document.querySelector('[data-error="content"]');
    elements.imageError = document.querySelector('[data-error="image"]');
  }

  // ============================================
  // Event Binding
  // ============================================
  function bindEvents() {
    // Form submit
    if (elements.form) {
      elements.form.addEventListener('submit', handleSubmit);
    }

    // Image input change
    if (elements.imageInput) {
      elements.imageInput.addEventListener('change', handleImageChange);
    }

    // Back button
    if (elements.backButton) {
      elements.backButton.addEventListener('click', handleBack);
    }

    // Cancel button
    if (elements.cancelButton) {
      elements.cancelButton.addEventListener('click', handleCancel);
    }

    // Real-time validation
    if (elements.titleInput) {
      elements.titleInput.addEventListener('input', validateTitle);
    }
  }

  // ============================================
  // Event Handlers
  // ============================================
  async function handleSubmit(e) {
    e.preventDefault();

    if (state.isSubmitting) return;

    // 검증
    if (!validateForm()) {
      return;
    }

    state.isSubmitting = true;
    setSubmitButtonState(true);

    try {
      // 1단계: 이미지 업로드 (있는 경우)
      if (state.selectedFile && !state.uploadedImageId) {
        try {
          const imageResult = await uploadImage(state.selectedFile);
          state.uploadedImageId = imageResult.imageId;      // camelCase 수정
          state.uploadedImageUrl = imageResult.imageUrl;    // camelCase 수정
        } catch (error) {
          console.error('Image upload failed:', error);
          showImageError(error.message);
          state.isSubmitting = false;
          setSubmitButtonState(false);
          return;
        }
      }

      // 2단계: 게시글 작성
      const title = elements.titleInput.value.trim();
      const content = elements.contentTextarea.value.trim();

      const post = await fetchWithAuth('/posts', {
        method: 'POST',
        body: JSON.stringify({
          title,
          content,
          imageId: state.uploadedImageId || null  // camelCase로 통일
        })
      });

      // 성공 - 상세 페이지로 이동
      window.location.href = `${CONFIG.DETAIL_URL}?id=${post.postId}`;

    } catch (error) {
      console.error('Failed to create post:', error);
      alert(translateErrorCode(error.message) || '게시글 작성에 실패했습니다.');
      state.isSubmitting = false;
      setSubmitButtonState(false);
    }
  }

  function handleImageChange(e) {
    const file = e.target.files[0];
    if (!file) {
      clearImagePreview();
      return;
    }

    // 파일 검증
    if (!validateImage(file)) {
      e.target.value = '';
      return;
    }

    // 이미지 미리보기
    state.selectedFile = file;
    showImagePreview(file);
    clearImageError();
  }

  function handleBack(e) {
    e.preventDefault();
    if (confirm('작성 중인 내용이 사라집니다. 나가시겠습니까?')) {
      window.history.back();
    }
  }

  function handleCancel(e) {
    e.preventDefault();
    if (confirm('작성 중인 내용이 사라집니다. 취소하시겠습니까?')) {
      window.location.href = CONFIG.LIST_URL;
    }
  }

  // ============================================
  // Validation Functions
  // ============================================
  function validateForm() {
    let isValid = true;

    // 제목 검증
    if (!validateTitle()) {
      isValid = false;
    }

    // 내용 검증
    if (!validateContent()) {
      isValid = false;
    }

    return isValid;
  }

  function validateTitle() {
    const title = elements.titleInput.value.trim();

    if (!title) {
      showTitleError('제목을 입력해주세요.');
      return false;
    }

    if (title.length > CONFIG.MAX_TITLE_LENGTH) {
      showTitleError(`제목은 ${CONFIG.MAX_TITLE_LENGTH}자 이하로 입력해주세요.`);
      return false;
    }

    clearTitleError();
    return true;
  }

  function validateContent() {
    const content = elements.contentTextarea.value.trim();

    if (!content) {
      showContentError('내용을 입력해주세요.');
      return false;
    }

    clearContentError();
    return true;
  }

  function validateImage(file) {
    // 파일 크기 검증 (5MB)
    if (file.size > CONFIG.MAX_FILE_SIZE) {
      showImageError('이미지 파일 크기는 5MB 이하여야 합니다.');
      return false;
    }

    // 파일 형식 검증 (JPG, PNG, GIF)
    if (!CONFIG.ALLOWED_FILE_TYPES.includes(file.type)) {
      showImageError('JPG, PNG, GIF 파일만 업로드 가능합니다.');
      return false;
    }

    return true;
  }

  // ============================================
  // Image Preview
  // ============================================
  function showImagePreview(file) {
    const reader = new FileReader();

    reader.onload = function(e) {
      if (elements.previewImage) {
        elements.previewImage.src = e.target.result;
        elements.previewImage.style.display = 'block';
      }
      if (elements.placeholder) {
        elements.placeholder.style.display = 'none';
      }
    };

    reader.readAsDataURL(file);
  }

  function clearImagePreview() {
    state.selectedFile = null;
    state.uploadedImageId = null;
    state.uploadedImageUrl = null;

    if (elements.previewImage) {
      elements.previewImage.src = '';
      elements.previewImage.style.display = 'none';
    }
    if (elements.placeholder) {
      elements.placeholder.style.display = 'flex';
    }
  }

  // ============================================
  // Error Display
  // ============================================
  function showTitleError(message) {
    if (elements.titleError) {
      elements.titleError.textContent = message;
      elements.titleError.style.display = 'block';
    }
    if (elements.titleInput) {
      elements.titleInput.classList.add('input-field__input--error');
    }
  }

  function clearTitleError() {
    if (elements.titleError) {
      elements.titleError.textContent = '';
      elements.titleError.style.display = 'none';
    }
    if (elements.titleInput) {
      elements.titleInput.classList.remove('input-field__input--error');
    }
  }

  function showContentError(message) {
    if (elements.contentError) {
      elements.contentError.textContent = message;
      elements.contentError.style.display = 'block';
    }
    if (elements.contentTextarea) {
      elements.contentTextarea.classList.add('input-field__textarea--error');
    }
  }

  function clearContentError() {
    if (elements.contentError) {
      elements.contentError.textContent = '';
      elements.contentError.style.display = 'none';
    }
    if (elements.contentTextarea) {
      elements.contentTextarea.classList.remove('input-field__textarea--error');
    }
  }

  function showImageError(message) {
    if (elements.imageError) {
      elements.imageError.textContent = translateErrorCode(message);
      elements.imageError.style.display = 'block';
    }
  }

  function clearImageError() {
    if (elements.imageError) {
      elements.imageError.textContent = '';
      elements.imageError.style.display = 'none';
    }
  }

  // ============================================
  // UI Helper
  // ============================================
  function setSubmitButtonState(isSubmitting) {
    if (elements.submitButton) {
      elements.submitButton.disabled = isSubmitting;
      elements.submitButton.textContent = isSubmitting ? '작성 중...' : '작성완료';
    }
  }

  // ============================================
  // DOMContentLoaded Event
  // ============================================
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

})(window, document);
