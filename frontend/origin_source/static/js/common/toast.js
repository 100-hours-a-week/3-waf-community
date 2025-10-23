/**
 * Toast Message System
 * 사용자 친화적 알림 메시지 표시
 *
 * 사용 예시:
 * - showSuccessToast('저장되었습니다.', '프로필 정보')
 * - showErrorToast('오류가 발생했습니다.', '다시 시도해주세요.')
 * - showInfoToast('안내', '댓글이 등록되었습니다.')
 */

const Toast = (function() {
  'use strict';

  // 설정
  const CONFIG = {
    DEFAULT_DURATION: 3000,  // 3초
    ANIMATION_DURATION: 300  // 애니메이션 시간 (ms)
  };

  // 토스트 컨테이너 생성
  function ensureContainer() {
    let container = document.querySelector('.toast-container');
    if (!container) {
      container = document.createElement('div');
      container.className = 'toast-container';
      document.body.appendChild(container);
    }
    return container;
  }

  // SVG 아이콘 생성
  function getIcon(type) {
    const icons = {
      success: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M20 6L9 17l-5-5" stroke-width="2" stroke-linecap="round"/></svg>',
      error: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01" stroke-width="2" stroke-linecap="round"/></svg>',
      warning: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M12 2L2 20h20L12 2z" stroke-width="2" stroke-linecap="round"/><path d="M12 9v4M12 17h.01" stroke-width="2"/></svg>',
      info: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01" stroke-width="2" stroke-linecap="round"/></svg>'
    };
    return icons[type] || icons.info;
  }

  // 클로즈 버튼 SVG
  function getCloseIcon() {
    return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><line x1="18" y1="6" x2="6" y2="18" stroke-width="2" stroke-linecap="round"/><line x1="6" y1="6" x2="18" y2="18" stroke-width="2" stroke-linecap="round"/></svg>';
  }

  // 토스트 생성 및 표시
  function show(options) {
    const {
      type = 'info',
      title = '',
      message = '',
      duration = CONFIG.DEFAULT_DURATION,
      onClose = null
    } = options;

    const container = ensureContainer();

    // 토스트 엘리먼트 생성
    const toast = document.createElement('div');
    toast.className = `toast toast--${type}`;

    // 토스트 HTML
    const titleHtml = title ? `<div class="toast__title">${escapeHtml(title)}</div>` : '';
    const messageHtml = message ? `<div class="toast__message">${escapeHtml(message)}</div>` : '';

    toast.innerHTML = `
      <div class="toast__icon">${getIcon(type)}</div>
      <div class="toast__content">
        ${titleHtml}
        ${messageHtml}
      </div>
      <button class="toast__close" aria-label="닫기">${getCloseIcon()}</button>
    `;

    // 닫기 버튼 이벤트
    const closeButton = toast.querySelector('.toast__close');
    if (closeButton) {
      closeButton.addEventListener('click', () => {
        removeToast(toast, onClose);
      });
    }

    // 컨테이너에 추가
    container.appendChild(toast);

    // 자동 제거
    if (duration > 0) {
      setTimeout(() => {
        if (toast.parentNode) {
          removeToast(toast, onClose);
        }
      }, duration);
    }

    return toast;
  }

  // 토스트 제거 (애니메이션 포함)
  function removeToast(toast, onClose) {
    if (!toast.parentNode) return;

    // Exit 애니메이션 추가
    toast.classList.add('toast--exiting');

    // 애니메이션 완료 후 제거
    setTimeout(() => {
      if (toast.parentNode) {
        toast.parentNode.removeChild(toast);
      }
      if (typeof onClose === 'function') {
        onClose();
      }
    }, CONFIG.ANIMATION_DURATION);
  }

  // XSS 방지
  function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  // Public API
  return {
    /**
     * 성공 토스트 표시
     * @param {string} message - 메시지 내용
     * @param {string} title - 제목 (선택)
     * @param {number} duration - 표시 시간 (ms)
     * @param {function} onClose - 닫힐 때 콜백
     */
    success: function(message, title = '성공', duration = CONFIG.DEFAULT_DURATION, onClose = null) {
      return show({
        type: 'success',
        title,
        message,
        duration,
        onClose
      });
    },

    /**
     * 에러 토스트 표시
     * @param {string} message - 메시지 내용
     * @param {string} title - 제목 (선택)
     * @param {number} duration - 표시 시간 (ms)
     * @param {function} onClose - 닫힐 때 콜백
     */
    error: function(message, title = '오류', duration = CONFIG.DEFAULT_DURATION, onClose = null) {
      return show({
        type: 'error',
        title,
        message,
        duration,
        onClose
      });
    },

    /**
     * 경고 토스트 표시
     * @param {string} message - 메시지 내용
     * @param {string} title - 제목 (선택)
     * @param {number} duration - 표시 시간 (ms)
     * @param {function} onClose - 닫힐 때 콜백
     */
    warning: function(message, title = '경고', duration = CONFIG.DEFAULT_DURATION, onClose = null) {
      return show({
        type: 'warning',
        title,
        message,
        duration,
        onClose
      });
    },

    /**
     * 정보 토스트 표시
     * @param {string} message - 메시지 내용
     * @param {string} title - 제목 (선택)
     * @param {number} duration - 표시 시간 (ms)
     * @param {function} onClose - 닫힐 때 콜백
     */
    info: function(message, title = '안내', duration = CONFIG.DEFAULT_DURATION, onClose = null) {
      return show({
        type: 'info',
        title,
        message,
        duration,
        onClose
      });
    }
  };
})();
