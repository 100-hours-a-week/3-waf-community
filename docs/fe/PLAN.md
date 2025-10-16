# Frontend Implementation Plan

## 문서 정보

| 항목 | 내용 |
|------|------|
| 작성일 | 2025-10-16 |
| 용도 | 프론트엔드 작업 현황 및 TODO 관리 |
| 참조 | **@CLAUDE.md** (구현 방법), **@docs/be/API.md** (API 명세) |

---

## 1. 현재 상태

### 1.1 파일 생성 현황

```
src/main/resources/
├── templates/ (12개 HTML) ✅ 100%
├── static/css/ (20개 CSS) ✅ 100%
└── static/js/ (11개 JS) ⚠️ 0% 구현
```

### 1.2 JavaScript 구현 상태

| 파일 | 상태 | 이슈 |
|------|------|------|
| `/js/common/api.js` | ⚠️ 부분 구현 | fetchWithAuth 패턴 불일치 |
| `/js/common/utils.js` | ❌ 미구현 | - |
| `/js/common/validation.js` | ❌ 미구현 | - |
| `/js/pages/board/*.js` (4개) | ❌ 주석만 | - |
| `/js/pages/user/*.js` (4개) | ❌ 주석만 | - |

**주요 문제점**:
1. `api.js`: 401 토큰 갱신 로직 없음 → @CLAUDE.md Section 3.1 패턴 필요
2. 모든 페이지 JS: 주석만 있고 실제 구현 없음
3. HTML: 샘플 데이터 하드코딩, 동적 렌더링 없음

---

## 2. 구현 우선순위

### Phase 1: Core Infrastructure (필수 선행)
- [ ] **Task 1.1**: api.js 재구현 (4h)
  - fetchWithAuth, refreshAccessToken, logout 구현
  - 참조: @CLAUDE.md Section 3.1, 3.2

- [ ] **Task 1.2**: utils.js 구현 (2h)
  - escapeHtml, formatDate, showError 등
  - 참조: @CLAUDE.md Section 5.3

- [ ] **Task 1.3**: validation.js 구현 (2h)
  - isValidEmail, isValidPassword 등
  - 참조: @CLAUDE.md Section 5.2

### Phase 2: Authentication Flow
- [ ] **Task 2.1**: login.js 구현 (3h)
  - 참조: @CLAUDE.md Section 4.4, @docs/be/API.md Section 1.1

- [ ] **Task 2.2**: register.js 구현 (4h)
  - 참조: @CLAUDE.md Section 4.5, @docs/be/API.md Section 2.1

### Phase 3: Board List & Detail
- [ ] **Task 3.1**: list.js 구현 (5h)
  - Cursor 페이지네이션
  - 참조: @CLAUDE.md Section 4.1, @docs/be/API.md Section 3.1

- [ ] **Task 3.2**: detail.js 구현 (6h)
  - 게시글 상세, 좋아요, 댓글 CRUD
  - 참조: @CLAUDE.md Section 4.2, @docs/be/API.md Section 3.2, 5, 6

### Phase 4: Post Write & Edit
- [ ] **Task 4.1**: write.js 구현 (5h)
  - 이미지 업로드 → 게시글 작성 (2단계)
  - 참조: @CLAUDE.md Section 4.3, @docs/be/API.md Section 3.3, 4.1

- [ ] **Task 4.2**: edit.js 구현 (4h)
  - 참조: @docs/be/API.md Section 3.4

### Phase 5: User Profile
- [ ] **Task 5.1**: profile-edit.js 구현 (4h)
  - 참조: @docs/be/API.md Section 2.2, 2.3

- [ ] **Task 5.2**: password-change.js 구현 (3h)
  - 참조: @docs/be/API.md Section 2.4
---

## 3. 의존성 그래프

```
Phase 1 (api.js, utils.js, validation.js)
  ├─→ Phase 2 (login.js, register.js)
  ├─→ Phase 3 (list.js, detail.js)
  ├─→ Phase 4 (write.js, edit.js)
  └─→ Phase 5 (profile-edit.js, password-change.js)
```

**Phase 1 완료 전에는 다른 작업 불가** (모든 페이지가 api.js, utils.js, validation.js에 의존)

---

## 4. 다음 단계

### 즉시 시작 가능 (블로킹 이슈 없음)

1. **api.js 재구현** (최우선)
   - 파일: `/js/common/api.js`
   - 현재 코드 삭제 후 @CLAUDE.md Section 3.1 패턴으로 재작성
   - 검증: 401 에러 시 토큰 갱신 확인

2. **utils.js, validation.js** (병렬 작업 가능)
   - @CLAUDE.md Section 5.2, 5.3 참조

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 |
|------|------|-----------|
| 2025-10-16 | 1.0 | 초기 PLAN.md 작성 (작업 현황 및 TODO만 포함) |
| 2025-10-16 | 1.1 | 모든 문서 참조 경로 절대 경로로 통일 (@CLAUDE.md, @docs/be/API.md) |
