/**
 * API Utility
 * fetch API 래퍼 및 공통 API 로직
 */

(function(window) {
  'use strict';

  const API = {
    /**
     * API 기본 설정
     */
    config: {
      baseURL: '/api',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json'
      }
    },

    /**
     * 토큰 가져오기
     */
    getToken() {
      return localStorage.getItem('authToken');
    },

    /**
     * 토큰 저장
     */
    setToken(token) {
      localStorage.setItem('authToken', token);
    },

    /**
     * 토큰 삭제
     */
    removeToken() {
      localStorage.removeItem('authToken');
    },

    /**
     * 로그인 상태 확인
     */
    isAuthenticated() {
      return !!this.getToken();
    },

    /**
     * Request 헤더 생성
     */
    getHeaders(customHeaders = {}) {
      const headers = { ...this.config.headers };

      const token = this.getToken();
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      return { ...headers, ...customHeaders };
    },

    /**
     * GET 요청
     */
    async get(endpoint, options = {}) {
      return this.request(endpoint, {
        method: 'GET',
        ...options
      });
    },

    /**
     * POST 요청
     */
    async post(endpoint, data, options = {}) {
      return this.request(endpoint, {
        method: 'POST',
        body: JSON.stringify(data),
        ...options
      });
    },

    /**
     * PUT 요청
     */
    async put(endpoint, data, options = {}) {
      return this.request(endpoint, {
        method: 'PUT',
        body: JSON.stringify(data),
        ...options
      });
    },

    /**
     * DELETE 요청
     */
    async delete(endpoint, options = {}) {
      return this.request(endpoint, {
        method: 'DELETE',
        ...options
      });
    },

    /**
     * FormData 업로드 (파일 포함)
     */
    async upload(endpoint, formData, options = {}) {
      const headers = this.getHeaders();
      delete headers['Content-Type']; // FormData는 자동으로 설정됨

      return this.request(endpoint, {
        method: 'POST',
        headers,
        body: formData,
        ...options
      });
    },

    /**
     * 공통 Request 메서드
     */
    async request(endpoint, options = {}) {
      const url = endpoint.startsWith('http')
        ? endpoint
        : `${this.config.baseURL}${endpoint}`;

      const config = {
        headers: this.getHeaders(options.headers),
        ...options
      };

      try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), this.config.timeout);

        const response = await fetch(url, {
          ...config,
          signal: controller.signal
        });

        clearTimeout(timeoutId);

        // 응답 처리
        const data = await this.handleResponse(response);
        return data;

      } catch (error) {
        return this.handleError(error);
      }
    },

    /**
     * 응답 처리
     */
    async handleResponse(response) {
      const contentType = response.headers.get('content-type');

      // JSON 응답
      if (contentType && contentType.includes('application/json')) {
        const data = await response.json();

        if (!response.ok) {
          throw new Error(data.error || data.message || 'API 오류가 발생했습니다.');
        }

        return data;
      }

      // 텍스트 응답
      const text = await response.text();
      if (!response.ok) {
        throw new Error(text || 'API 오류가 발생했습니다.');
      }

      return { data: text };
    },

    /**
     * 에러 처리
     */
    handleError(error) {
      console.error('API Error:', error);

      if (error.name === 'AbortError') {
        throw new Error('요청 시간이 초과되었습니다.');
      }

      if (error.message === 'Failed to fetch') {
        throw new Error('네트워크 연결을 확인해주세요.');
      }

      throw error;
    }
  };

  // Export
  window.API = API;

})(window);
