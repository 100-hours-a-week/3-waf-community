/**
 * Common Utilities
 * 공통 유틸리티 함수들
 */

(function(window) {
  'use strict';

  const Utils = {
    /**
     * 날짜 포맷팅
     * @param {string|Date} date - 날짜
     * @param {string} format - 'YYYY-MM-DD', 'YYYY-MM-DD HH:mm:ss' 등
     */
    formatDate(date, format = 'YYYY-MM-DD HH:mm:ss') {
      const d = new Date(date);

      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      const hours = String(d.getHours()).padStart(2, '0');
      const minutes = String(d.getMinutes()).padStart(2, '0');
      const seconds = String(d.getSeconds()).padStart(2, '0');

      return format
        .replace('YYYY', year)
        .replace('MM', month)
        .replace('DD', day)
        .replace('HH', hours)
        .replace('mm', minutes)
        .replace('ss', seconds);
    },

    /**
     * 숫자 포맷팅 (1k, 10k, 100k)
     */
    formatNumber(num) {
      if (num >= 100000) {
        return Math.floor(num / 1000) + 'k';
      }
      if (num >= 10000) {
        return Math.floor(num / 1000) + 'k';
      }
      if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'k';
      }
      return num.toString();
    },

    /**
     * 디바운스
     */
    debounce(func, wait = 300) {
      let timeout;
      return function executedFunction(...args) {
        const later = () => {
          clearTimeout(timeout);
          func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
      };
    },

    /**
     * 쓰로틀
     */
    throttle(func, limit = 300) {
      let inThrottle;
      return function(...args) {
        if (!inThrottle) {
          func.apply(this, args);
          inThrottle = true;
          setTimeout(() => inThrottle = false, limit);
        }
      };
    },

    /**
     * URL 쿼리 파라미터 파싱
     */
    parseQuery(queryString = window.location.search) {
      const params = new URLSearchParams(queryString);
      const result = {};
      for (const [key, value] of params) {
        result[key] = value;
      }
      return result;
    },

    /**
     * URL 쿼리 파라미터 생성
     */
    buildQuery(params) {
      const query = new URLSearchParams(params);
      return query.toString();
    },

    /**
     * 로컬 스토리지 헬퍼
     */
    storage: {
      get(key) {
        try {
          const item = localStorage.getItem(key);
          return item ? JSON.parse(item) : null;
        } catch (e) {
          console.error('Storage get error:', e);
          return null;
        }
      },

      set(key, value) {
        try {
          localStorage.setItem(key, JSON.stringify(value));
          return true;
        } catch (e) {
          console.error('Storage set error:', e);
          return false;
        }
      },

      remove(key) {
        try {
          localStorage.removeItem(key);
          return true;
        } catch (e) {
          console.error('Storage remove error:', e);
          return false;
        }
      },

      clear() {
        try {
          localStorage.clear();
          return true;
        } catch (e) {
          console.error('Storage clear error:', e);
          return false;
        }
      }
    },

    /**
     * Toast 메시지
     */
    toast: {
      show(message, type = 'info', duration = 3000) {
        // 기존 토스트 제거
        const existingToast = document.querySelector('[data-toast]');
        if (existingToast) {
          existingToast.remove();
        }

        // 토스트 생성
        const toast = document.createElement('div');
        toast.className = `toast toast--${type}`;
        toast.setAttribute('data-toast', '');
        toast.innerHTML = `
          <span class="toast__message">${message}</span>
          <button class="toast__close" data-toast-close>×</button>
        `;

        // 스타일 적용 (인라인)
        Object.assign(toast.style, {
          position: 'fixed',
          bottom: '24px',
          right: '24px',
          padding: '16px 24px',
          backgroundColor: type === 'error' ? '#EF4444' : '#3B82F6',
          color: '#FFFFFF',
          borderRadius: '8px',
          boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
          zIndex: '1080',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          animation: 'slideIn 0.3s ease-out'
        });

        // 닫기 버튼
        const closeBtn = toast.querySelector('[data-toast-close]');
        closeBtn.style.background = 'none';
        closeBtn.style.border = 'none';
        closeBtn.style.color = '#FFFFFF';
        closeBtn.style.fontSize = '24px';
        closeBtn.style.cursor = 'pointer';
        closeBtn.style.padding = '0';
        closeBtn.style.lineHeight = '1';

        closeBtn.addEventListener('click', () => {
          toast.remove();
        });

        document.body.appendChild(toast);

        // 자동 제거
        if (duration > 0) {
          setTimeout(() => {
            if (toast.parentElement) {
              toast.remove();
            }
          }, duration);
        }
      },

      success(message, duration) {
        this.show(message, 'success', duration);
      },

      error(message, duration) {
        this.show(message, 'error', duration);
      }
    },

    /**
     * 모달 헬퍼
     */
    modal: {
      show(modalElement) {
        if (!modalElement) return;
        modalElement.removeAttribute('hidden');
        modalElement.style.display = 'flex';
        document.body.style.overflow = 'hidden';
      },

      hide(modalElement) {
        if (!modalElement) return;
        modalElement.setAttribute('hidden', '');
        modalElement.style.display = 'none';
        document.body.style.overflow = '';
      },

      confirm(title, description) {
        return new Promise((resolve) => {
          const modal = document.createElement('div');
          modal.className = 'modal';
          modal.setAttribute('data-modal', '');
          modal.innerHTML = `
            <div class="modal__backdrop"></div>
            <div class="modal__container">
              <div class="modal__content">
                <h3 class="modal__title">${title}</h3>
                <p class="modal__description">${description}</p>
                <div class="modal__actions">
                  <button class="btn btn--secondary" data-action="cancel">취소</button>
                  <button class="btn btn--primary" data-action="confirm">확인</button>
                </div>
              </div>
            </div>
          `;

          // 스타일 적용
          Object.assign(modal.style, {
            display: 'flex',
            position: 'fixed',
            top: '0',
            left: '0',
            width: '100%',
            height: '100%',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: '1050'
          });

          const backdrop = modal.querySelector('.modal__backdrop');
          Object.assign(backdrop.style, {
            position: 'absolute',
            top: '0',
            left: '0',
            width: '100%',
            height: '100%',
            backgroundColor: 'rgba(0, 0, 0, 0.5)'
          });

          const container = modal.querySelector('.modal__container');
          Object.assign(container.style, {
            position: 'relative',
            maxWidth: '500px',
            width: '90%',
            backgroundColor: '#0F0F0F',
            borderRadius: '12px',
            padding: '24px',
            zIndex: '1051'
          });

          document.body.appendChild(modal);

          // 이벤트 리스너
          modal.querySelector('[data-action="cancel"]').addEventListener('click', () => {
            modal.remove();
            resolve(false);
          });

          modal.querySelector('[data-action="confirm"]').addEventListener('click', () => {
            modal.remove();
            resolve(true);
          });

          backdrop.addEventListener('click', () => {
            modal.remove();
            resolve(false);
          });
        });
      }
    }
  };

  // Export
  window.Utils = Utils;

})(window);
