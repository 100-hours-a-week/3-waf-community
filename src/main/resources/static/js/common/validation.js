/**
 * Validation Utility
 * 클라이언트 검증 함수
 * 참조: @CLAUDE.md Section 5.2, @docs/be/LLD.md Section 6.4
 */

/**
 * 이메일 형식 검증
 * RFC 5322 간소화 버전
 *
 * @param {string} email
 * @returns {boolean}
 */
function isValidEmail(email) {
    if (!email || typeof email !== 'string') return false;

    const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return regex.test(email.trim());
}

/**
 * 비밀번호 정책 검증
 * - 8-20자
 * - 대문자 최소 1개 ([A-Z])
 * - 소문자 최소 1개 ([a-z])
 * - 특수문자 최소 1개 ([!@#$%^&*(),.?":{}|<>])
 *
 * 참조: @docs/be/LLD.md Section 6.4
 *
 * @param {string} password
 * @returns {boolean}
 */
function isValidPassword(password) {
    if (!password || typeof password !== 'string') return false;

    // 길이 검증
    if (password.length < 8 || password.length > 20) return false;

    // 대문자, 소문자, 특수문자 각 1개 이상
    const hasUpper = /[A-Z]/.test(password);
    const hasLower = /[a-z]/.test(password);
    const hasSpecial = /[!@#$%^&*(),.?":{}|<>]/.test(password);

    return hasUpper && hasLower && hasSpecial;
}

/**
 * 닉네임 검증
 * 1-10자 (한글 기준)
 *
 * 참조: @docs/be/DDL.md (users 테이블, CHECK 제약)
 *
 * @param {string} nickname
 * @returns {boolean}
 */
function isValidNickname(nickname) {
    if (!nickname || typeof nickname !== 'string') return false;

    const trimmed = nickname.trim();
    return trimmed.length >= 1 && trimmed.length <= 10;
}

/**
 * 게시글 제목 검증
 * 1-27자 (한글 기준)
 *
 * 참조: @docs/be/DDL.md (posts 테이블, CHECK 제약)
 *
 * @param {string} title
 * @returns {boolean}
 */
function isValidTitle(title) {
    if (!title || typeof title !== 'string') return false;

    const trimmed = title.trim();
    return trimmed.length >= 1 && trimmed.length <= 27;
}

/**
 * 댓글 검증
 * 1-200자 (한글 기준)
 *
 * 참조: @docs/be/DDL.md (comments 테이블, CHECK 제약)
 *
 * @param {string} content
 * @returns {boolean}
 */
function isValidComment(content) {
    if (!content || typeof content !== 'string') return false;

    const trimmed = content.trim();
    return trimmed.length >= 1 && trimmed.length <= 200;
}

/**
 * 비밀번호 확인 일치 검증
 *
 * @param {string} password
 * @param {string} confirmPassword
 * @returns {boolean}
 */
function isPasswordMatch(password, confirmPassword) {
    if (!password || !confirmPassword) return false;
    return password === confirmPassword;
}

/**
 * 이미지 파일 검증
 * - 최대 5MB
 * - JPG/PNG/GIF만 허용
 *
 * 참조: @docs/be/API.md Section 4.1 (IMAGE-002, IMAGE-003)
 *
 * @param {File} file
 * @returns {boolean}
 */
function isValidImageFile(file) {
    if (!file) return false;

    // 5MB 제한
    const maxSize = 5 * 1024 * 1024;
    if (file.size > maxSize) return false;

    // JPG/PNG/GIF만 허용
    const allowedTypes = ['image/jpeg', 'image/png', 'image/gif'];
    return allowedTypes.includes(file.type);
}

/**
 * 이미지 파일 에러 메시지
 * 검증 실패 시 사용자에게 보여줄 메시지 반환
 *
 * @param {File} file
 * @returns {string|null} - 에러 메시지 (문제 없으면 null)
 */
function getImageFileError(file) {
    if (!file) return '파일을 선택해주세요.';

    const maxSize = 5 * 1024 * 1024;
    if (file.size > maxSize) return '이미지 파일 크기는 5MB 이하여야 합니다.';

    const allowedTypes = ['image/jpeg', 'image/png', 'image/gif'];
    if (!allowedTypes.includes(file.type)) {
        return 'JPG, PNG, GIF 파일만 업로드 가능합니다.';
    }

    return null;
}

/**
 * 비밀번호 정책 설명
 * 에러 메시지로 사용
 *
 * @returns {string}
 */
function getPasswordPolicyMessage() {
    return '비밀번호는 8-20자, 대문자/소문자/특수문자를 각각 1개 이상 포함해야 합니다.';
}
