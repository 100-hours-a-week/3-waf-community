/**
 * Common Utilities
 * 공통 유틸리티 함수
 * 참조: @CLAUDE.md Section 5.3
 */

/**
 * XSS 방지 HTML 이스케이프
 * 사용자 입력을 DOM에 삽입할 때 사용
 *
 * @param {string} str
 * @returns {string}
 */
function escapeHtml(str) {
    if (!str || typeof str !== 'string') return '';

    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

/**
 * 상대 시간 표시
 * ISO 8601 문자열 → "2시간 전", "1일 전"
 *
 * @param {string} dateString - ISO 8601 (예: "2025-10-16T10:00:00Z")
 * @returns {string}
 */
function formatDate(dateString) {
    if (!dateString) return '';

    const date = new Date(dateString);
    const now = new Date();
    const diff = Math.floor((now - date) / 1000); // 초 단위

    if (diff < 60) return '방금 전';
    if (diff < 3600) return `${Math.floor(diff / 60)}분 전`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`;
    if (diff < 604800) return `${Math.floor(diff / 86400)}일 전`;

    // 1주일 이상: "2025.10.16"
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}.${month}.${day}`;
}

/**
 * 천 단위 콤마
 * 1234 → "1,234"
 *
 * @param {number} num
 * @returns {string}
 */
function formatNumber(num) {
    if (typeof num !== 'number') return '0';
    return num.toLocaleString('ko-KR');
}

/**
 * 숫자 포맷 (축약형, 소셜 미디어 스타일)
 * 1234 → "1.2k", 10000 → "10k"
 *
 * @param {number} num
 * @returns {string}
 */
function formatNumberCompact(num) {
    if (typeof num !== 'number') return '0';
    if (num >= 10000) return `${Math.floor(num / 1000)}k`;
    if (num >= 1000) return `${(num / 1000).toFixed(1)}k`;
    return num.toString();
}

/**
 * 에러 메시지 표시
 * 현재: alert (추후 모달/토스트로 개선)
 *
 * @param {string} message
 */
function showError(message) {
    alert(message); // TODO: 모달 또는 토스트로 개선
}

/**
 * 성공 메시지 표시
 * 현재: alert (추후 모달/토스트로 개선)
 *
 * @param {string} message
 */
function showSuccess(message) {
    alert(message); // TODO: 모달 또는 토스트로 개선
}

/**
 * 로딩 상태 표시/숨김
 * data-loading 속성을 가진 요소를 토글
 *
 * @param {boolean} show - true: 표시, false: 숨김
 * @param {string} selector - 선택자 (기본: '[data-loading]')
 */
function showLoading(show, selector = '[data-loading]') {
    const loadingElement = document.querySelector(selector);
    if (!loadingElement) return;

    if (show) {
        loadingElement.style.display = 'flex';
    } else {
        loadingElement.style.display = 'none';
    }
}

/**
 * 절대 날짜 포맷팅
 * 날짜 객체 → "2025-10-16 14:30:00"
 *
 * @param {Date|string} date
 * @param {string} format - 'YYYY-MM-DD', 'YYYY-MM-DD HH:mm:ss' 등
 * @returns {string}
 */
function formatAbsoluteDate(date, format = 'YYYY-MM-DD HH:mm:ss') {
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
}

/**
 * 디바운스
 * 연속된 이벤트를 하나로 그룹화 (마지막만 실행)
 *
 * @param {Function} func
 * @param {number} wait - 대기 시간 (ms)
 * @returns {Function}
 */
function debounce(func, wait = 300) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

/**
 * 쓰로틀
 * 일정 시간마다 한 번만 실행
 *
 * @param {Function} func
 * @param {number} limit - 제한 시간 (ms)
 * @returns {Function}
 */
function throttle(func, limit = 300) {
    let inThrottle;
    return function(...args) {
        if (!inThrottle) {
            func.apply(this, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

/**
 * URL 쿼리 파라미터 파싱
 * "?id=123&sort=latest" → { id: "123", sort: "latest" }
 *
 * @param {string} queryString - 쿼리 문자열 (기본: window.location.search)
 * @returns {Object}
 */
function parseQuery(queryString = window.location.search) {
    const params = new URLSearchParams(queryString);
    const result = {};
    for (const [key, value] of params) {
        result[key] = value;
    }
    return result;
}

/**
 * URL 쿼리 파라미터 생성
 * { id: "123", sort: "latest" } → "id=123&sort=latest"
 *
 * @param {Object} params
 * @returns {string}
 */
function buildQuery(params) {
    const query = new URLSearchParams(params);
    return query.toString();
}

/**
 * 확인 모달
 * Promise 기반 confirm 대체
 *
 * @param {string} title - 제목
 * @param {string} description - 설명
 * @returns {Promise<boolean>} - 확인: true, 취소: false
 */
function confirmModal(title, description) {
    return new Promise((resolve) => {
        const modal = document.createElement('div');
        modal.className = 'modal';
        modal.setAttribute('data-modal', '');
        modal.innerHTML = `
            <div class="modal__backdrop"></div>
            <div class="modal__container">
                <div class="modal__content">
                    <h3 class="modal__title">${escapeHtml(title)}</h3>
                    <p class="modal__description">${escapeHtml(description)}</p>
                    <div class="modal__actions">
                        <button class="btn btn--secondary" data-action="cancel">취소</button>
                        <button class="btn btn--primary" data-action="confirm">확인</button>
                    </div>
                </div>
            </div>
        `;

        // 스타일 적용 (인라인)
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
            backgroundColor: '#FFFFFF',
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
