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
    DETAIL_URL: '/pages/board/detail.html'
  };

  const state = {
    postId: null,
    originalPost: null,
    uploadedImageId: null,
    selectedFile: null,
    removeExistingImage: false,
    isSubmitting: false
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
    titleError: null,
    contentError: null,
    imageError: null
  };

  function init() {
    const urlParams = new URLSearchParams(window.location.search);
    state.postId = urlParams.get('id');

    if (!state.postId) {
      alert('게시글을 찾을 수 없습니다.');
      window.history.back();
      return;
    }

    cacheElements();
    bindEvents();
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
    elements.titleError = document.querySelector('[data-error="title"]');
    elements.contentError = document.querySelector('[data-error="content"]');
    elements.imageError = document.querySelector('[data-error="image"]');
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
    document.querySelector('[data-action="go-back"]')?.addEventListener('click', e => {
      e.preventDefault();
      if (confirm('수정 중인 내용이 사라집니다. 나가시겠습니까?')) {
        window.location.href = `${CONFIG.DETAIL_URL}?id=${state.postId}`;
      }
    });
    document.querySelector('[data-action="cancel"]')?.addEventListener('click', e => {
      e.preventDefault();
      if (confirm('수정 중인 내용이 사라집니다. 취소하시겠습니까?')) {
        window.location.href = `${CONFIG.DETAIL_URL}?id=${state.postId}`;
      }
    });
  }

  async function loadPost() {
    try {
      const post = await fetchWithAuth(`/posts/${state.postId}`);
      state.originalPost = post;

      // 폼 채우기
      elements.titleInput.value = post.title;
      elements.contentTextarea.value = post.content;

      // 기존 이미지 표시
      if (post.imageUrl) {
        elements.previewImage.src = post.imageUrl;
        elements.previewImage.style.display = 'block';
        elements.placeholder.style.display = 'none';
        elements.removeImageButton.style.display = 'block';
        state.uploadedImageId = post.imageId;
      }
    } catch (error) {
      console.error('Failed to load post:', error);
      alert('게시글을 불러오는데 실패했습니다.');
      window.history.back();
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (state.isSubmitting) return;

    if (!validateForm()) return;

    state.isSubmitting = true;
    elements.submitButton.disabled = true;
    elements.submitButton.textContent = '수정 중...';

    try {
      // 새 이미지 업로드
      if (state.selectedFile) {
        const imageResult = await uploadImage(state.selectedFile);
        state.uploadedImageId = imageResult.imageId;  // camelCase 수정
      }

      // 게시글 수정 (PATCH)
      const body = {
        title: elements.titleInput.value.trim(),
        content: elements.contentTextarea.value.trim()
      };

      // 이미지 처리
      if (state.removeExistingImage) {
        body.imageId = null;  // camelCase로 통일
      } else if (state.uploadedImageId) {
        body.imageId = state.uploadedImageId;  // camelCase로 통일
      }

      await fetchWithAuth(`/posts/${state.postId}`, {
        method: 'PATCH',
        body: JSON.stringify(body)
      });

      window.location.href = `${CONFIG.DETAIL_URL}?id=${state.postId}`;
    } catch (error) {
      console.error('Failed to update post:', error);
      alert(translateErrorCode(error.message) || '게시글 수정에 실패했습니다.');
      state.isSubmitting = false;
      elements.submitButton.disabled = false;
      elements.submitButton.textContent = '수정완료';
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
  }

  function handleRemoveImage() {
    state.selectedFile = null;
    state.uploadedImageId = null;
    state.removeExistingImage = true;

    elements.previewImage.src = '';
    elements.previewImage.style.display = 'none';
    elements.placeholder.style.display = 'flex';
    elements.removeImageButton.style.display = 'none';
    elements.imageInput.value = '';
  }

  function validateForm() {
    let isValid = true;
    const title = elements.titleInput.value.trim();
    const content = elements.contentTextarea.value.trim();

    if (!title) {
      elements.titleError.textContent = '제목을 입력해주세요.';
      elements.titleError.style.display = 'block';
      isValid = false;
    } else if (title.length > CONFIG.MAX_TITLE_LENGTH) {
      elements.titleError.textContent = `제목은 ${CONFIG.MAX_TITLE_LENGTH}자 이하로 입력해주세요.`;
      elements.titleError.style.display = 'block';
      isValid = false;
    } else {
      elements.titleError.style.display = 'none';
    }

    if (!content) {
      elements.contentError.textContent = '내용을 입력해주세요.';
      elements.contentError.style.display = 'block';
      isValid = false;
    } else {
      elements.contentError.style.display = 'none';
    }

    return isValid;
  }

  function validateImage(file) {
    if (file.size > CONFIG.MAX_FILE_SIZE) {
      elements.imageError.textContent = '이미지 파일 크기는 5MB 이하여야 합니다.';
      elements.imageError.style.display = 'block';
      return false;
    }
    if (!CONFIG.ALLOWED_FILE_TYPES.includes(file.type)) {
      elements.imageError.textContent = 'JPG, PNG, GIF 파일만 업로드 가능합니다.';
      elements.imageError.style.display = 'block';
      return false;
    }
    elements.imageError.style.display = 'none';
    return true;
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

})(window, document);
