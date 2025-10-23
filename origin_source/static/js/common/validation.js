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

// ============================================
// 스팸/도배 검증 함수
// ============================================

/**
 * 연속된 동일 문자 검증
 * 같은 문자가 threshold번 이상 반복되면 도배로 간주
 *
 * @param {string} str - 검사할 문자열
 * @param {number} threshold - 허용 반복 횟수 (기본 4)
 * @returns {boolean} - true면 정상, false면 도배
 */
function hasRepeatingCharacters(str, threshold = 4) {
    if (!str || typeof str !== 'string') return false;

    const regex = new RegExp(`(.)\\1{${threshold - 1},}`, 'g');
    return regex.test(str);
}

/**
 * 과도한 공백 검증
 * 연속 공백이 3개 이상이거나 앞뒤 공백만 있는 경우
 *
 * @param {string} str - 검사할 문자열
 * @returns {boolean} - true면 문제 있음
 */
function hasExcessiveWhitespace(str) {
    if (!str || typeof str !== 'string') return false;

    // 연속 공백 3개 이상
    if (/\s{3,}/.test(str)) return true;

    // trim 후 빈 문자열 (공백만 있음)
    if (str.trim().length === 0) return true;

    return false;
}

/**
 * 입력값 정제
 * - 앞뒤 공백 제거
 * - 연속된 공백을 하나로 축소
 *
 * @param {string} str - 정제할 문자열
 * @returns {string} - 정제된 문자열
 */
function sanitizeInput(str) {
    if (!str || typeof str !== 'string') return '';

    return str
        .trim()                    // 앞뒤 공백 제거
        .replace(/\s+/g, ' ');     // 연속 공백을 하나로
}

/**
 * 비밀번호 강도 계산
 * 점수 기반으로 weak/medium/strong 반환
 *
 * 점수 계산:
 * - 길이 8자 이상: +1
 * - 길이 12자 이상: +1
 * - 소문자: +1
 * - 대문자: +1
 * - 숫자: +1
 * - 특수문자: +1
 *
 * 강도:
 * - 0-2점: weak
 * - 3-4점: medium
 * - 5-6점: strong
 *
 * @param {string} password - 비밀번호
 * @returns {object} - { strength: 'weak|medium|strong', score: 0-6, percentage: 0-100 }
 */
function getPasswordStrength(password) {
    if (!password) return { strength: 'weak', score: 0, percentage: 0 };

    let score = 0;

    // 길이 점수
    if (password.length >= 8) score++;
    if (password.length >= 12) score++;

    // 문자 종류 점수
    if (/[a-z]/.test(password)) score++;  // 소문자
    if (/[A-Z]/.test(password)) score++;  // 대문자
    if (/[0-9]/.test(password)) score++;  // 숫자
    if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) score++;  // 특수문자

    // 강도 판정
    let strength;
    if (score <= 2) strength = 'weak';
    else if (score <= 4) strength = 'medium';
    else strength = 'strong';

    // 퍼센티지 계산 (0-100)
    const percentage = Math.round((score / 6) * 100);

    return { strength, score, percentage };
}

/**
 * 비밀번호 강도 메시지
 * 사용자에게 보여줄 텍스트
 *
 * @param {string} strength - 'weak' | 'medium' | 'strong'
 * @returns {string}
 */
function getPasswordStrengthMessage(strength) {
    const messages = {
        weak: '약함 - 더 안전한 비밀번호를 사용하세요',
        medium: '보통 - 괜찮은 비밀번호입니다',
        strong: '강함 - 안전한 비밀번호입니다'
    };

    return messages[strength] || '';
}
