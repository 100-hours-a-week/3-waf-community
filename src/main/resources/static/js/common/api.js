/**
 * API Utility
 * JWT 기반 인증 및 REST API 통신
 * 참조: @CLAUDE.md Section 3.1, 3.2
 */

// API Base URL
// 추후 서버 분리시, 환경 변수 처리 필수
const API_BASE_URL = 'http://localhost:8080';

/**
 * JWT 토큰을 자동으로 추가하는 Fetch 래퍼
 * - 401 Unauthorized 시 자동 토큰 갱신 및 재시도
 * - ApiResponse 형식 처리 (data 필드 자동 추출)
 *
 * @param {string} url - API 엔드포인트 (예: '/posts', '/auth/login')
 * @param {Object} options - fetch options
 * @returns {Promise<any>} - ApiResponse.data 또는 { success: true }
 */
async function fetchWithAuth(url, options = {}) {
    const token = localStorage.getItem('access_token');

    const config = {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            ...options.headers,
            ...(token && { Authorization: `Bearer ${token}` })
        }
    };

    try {
        const response = await fetch(`${API_BASE_URL}${url}`, config);

        // 401 Unauthorized → 에러 코드 우선 체크
        if (response.status === 401) {
            // ① 에러 코드 먼저 확인 (response.clone() 사용)
            const clonedResponse = response.clone();
            try {
                const errorData = await clonedResponse.json();

                // ② 로그인 실패 등 인증 정보 오류는 토큰 갱신하지 않고 바로 throw
                const authFailures = ['AUTH-001']; // Invalid credentials
                const errorCode = errorData.message?.match(/([A-Z]+-\d+)/)?.[1];

                if (authFailures.includes(errorCode)) {
                    throw new Error(errorData.message);
                }
            } catch (parseError) {
                // JSON 파싱 실패 또는 에러 throw 시
                if (parseError.message?.match(/([A-Z]+-\d+)/)) {
                    // 명시적으로 던진 에러면 그대로 throw
                    throw parseError;
                }
                // JSON 파싱 실패면 기존 로직 진행
                console.warn('Failed to parse 401 error response:', parseError);
            }

            // ③ 그 외 401(토큰 만료 등)은 기존 로직 유지
            const refreshed = await refreshAccessToken();
            if (refreshed) {
                // 토큰 갱신 성공 → 원래 요청 재시도
                const newToken = localStorage.getItem('access_token');
                config.headers.Authorization = `Bearer ${newToken}`;
                const retryResponse = await fetch(`${API_BASE_URL}${url}`, config);
                return handleResponse(retryResponse);
            } else {
                // 토큰 갱신 실패 → 로그인 페이지로 리다이렉트
                logout();
                window.location.href = '/pages/user/login.html';
                throw new Error('Authentication failed');
            }
        }

        return handleResponse(response);
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
}

/**
 * 응답 처리
 * - 204 No Content: { success: true } 반환
 * - 200 OK: ApiResponse.data 필드 반환
 * - 4xx/5xx: Error throw
 *
 * @param {Response} response - fetch Response 객체
 * @returns {Promise<any>}
 */
async function handleResponse(response) {
    if (response.status === 204) return { success: true };

    const data = await response.json();
    if (response.ok) return data.data; // ApiResponse의 data 필드 반환

    throw new Error(data.message || 'API request failed');
}

/**
 * Access Token 갱신
 * POST /auth/refresh_token 호출
 *
 * @returns {Promise<boolean>} 갱신 성공 여부
 */
async function refreshAccessToken() {
    const refreshToken = localStorage.getItem('refresh_token');
    if (!refreshToken) return false;

    try {
        const response = await fetch(`${API_BASE_URL}/auth/refresh_token`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refresh_token: refreshToken })
        });

        if (response.ok) {
            const data = await response.json();
            localStorage.setItem('access_token', data.data.access_token);
            return true;
        }
        return false;
    } catch (error) {
        console.error('Token refresh failed:', error);
        return false;
    }
}

/**
 * 로그아웃
 * - POST /auth/logout 호출 (서버에 refresh_token 무효화 요청)
 * - LocalStorage 토큰 삭제
 *
 * 참고: /auth/logout은 인증 필요 (SecurityConfig)
 */
async function logout() {
    const refreshToken = localStorage.getItem('refresh_token');
    const accessToken = localStorage.getItem('access_token');

    try {
        await fetch(`${API_BASE_URL}/auth/logout`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${accessToken}` // 인증 필요
            },
            body: JSON.stringify({ refresh_token: refreshToken })
        });
    } catch (error) {
        console.error('Logout request failed:', error);
    }

    // 로컬 스토리지 정리
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
}

/**
 * 로그인 여부 확인
 * @returns {boolean}
 */
function isAuthenticated() {
    return !!localStorage.getItem('access_token');
}

/**
 * JWT에서 userId 추출 (옵션)
 * JWT payload의 sub 필드에서 사용자 ID 추출
 *
 * @returns {number|null}
 */
function getCurrentUserId() {
    const token = localStorage.getItem('access_token');
    if (!token) return null;

    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return parseInt(payload.sub, 10);
    } catch (error) {
        console.error('Failed to parse JWT:', error);
        return null;
    }
}

/**
 * 이미지 업로드
 * POST /images (multipart/form-data)
 *
 * @param {File} file - 업로드할 이미지 파일
 * @returns {Promise<{image_id: number, image_url: string}>}
 */
async function uploadImage(file) {
    const formData = new FormData();
    formData.append('file', file);

    const token = localStorage.getItem('access_token');

    const response = await fetch(`${API_BASE_URL}/images`, {
        method: 'POST',
        headers: {
            Authorization: `Bearer ${token}` // Content-Type 자동 설정
        },
        body: formData
    });

    const data = await response.json();
    if (response.ok) return data.data; // { image_id, image_url }

    throw new Error(data.message);
}

/**
 * ErrorCode → 사용자 친화적 메시지 변환
 * 백엔드 ErrorCode (28개) → 한글 메시지
 * 참조: @docs/be/LLD.md Section 8
 *
 * @param {string} code - ErrorCode (예: "USER-002", "AUTH-001")
 * @returns {string} - 사용자 메시지
 */
function translateErrorCode(code) {
    const errorMessages = {
        // AUTH 에러
        'AUTH-001': '이메일 또는 비밀번호가 일치하지 않습니다.',
        'AUTH-002': '유효하지 않은 토큰입니다.',
        'AUTH-003': '토큰이 만료되었습니다.',
        'AUTH-004': '유효하지 않은 리프레시 토큰입니다.',

        // USER 에러
        'USER-001': '사용자를 찾을 수 없습니다.',
        'USER-002': '이미 사용 중인 이메일입니다.',
        'USER-003': '이미 사용 중인 닉네임입니다.',
        'USER-004': '비밀번호는 8-20자, 대/소/특수문자를 각각 1개 이상 포함해야 합니다.',
        'USER-005': '계정이 비활성화되었습니다.',
        'USER-006': '비밀번호가 일치하지 않습니다.',
        'USER-007': '권한이 없습니다.',

        // POST 에러
        'POST-001': '게시글을 찾을 수 없습니다.',
        'POST-002': '게시글 작성자만 수정/삭제할 수 있습니다.',
        'POST-003': '이미 삭제된 게시글입니다.',
        'POST-004': '유효하지 않은 게시글 상태입니다.',

        // COMMENT 에러
        'COMMENT-001': '댓글을 찾을 수 없습니다.',
        'COMMENT-002': '댓글 작성자만 수정/삭제할 수 있습니다.',
        'COMMENT-003': '이미 삭제된 댓글입니다.',

        // LIKE 에러
        'LIKE-001': '이미 좋아요를 눌렀습니다.',
        'LIKE-002': '좋아요를 찾을 수 없습니다.',

        // IMAGE 에러
        'IMAGE-001': '이미지를 찾을 수 없습니다.',
        'IMAGE-002': '파일 크기는 5MB 이하여야 합니다.',
        'IMAGE-003': '지원하지 않는 파일 형식입니다. (JPG, PNG, GIF만 가능)',

        // COMMON 에러
        'COMMON-001': '입력 데이터가 올바르지 않습니다.',
        'COMMON-002': '리소스를 찾을 수 없습니다.',
        'COMMON-003': '리소스 충돌이 발생했습니다.',
        'COMMON-004': '요청 횟수가 너무 많습니다. 잠시 후 다시 시도해주세요.',
        'COMMON-999': '서버 오류가 발생했습니다.'
    };

    // ErrorCode 추출 (예: "USER-002: Email already exists" → "USER-002")
    const match = code.match(/([A-Z]+-\d+)/);
    return match ? (errorMessages[match[1]] || code) : code;
}
