# 검증 완료 테스트 케이스 보고서

## 문서 정보

| 항목 | 내용 |
|------|------|
| 문서 유형 | QA 검증 완료 테스트 케이스 |
| 작성일 | 2025-10-19 |
| 검증 기간 | 2025-10-17 ~ 2025-10-18 |
| 검증 기준 | **완전 검증된 케이스만 포함** (부분 검증/미테스트 제외) |
| 총 API 수 | 21개 |
| 검증 완료 | **13개 (61.9%)** |
| 제외 | 8개 (38.1%) |

---

## Executive Summary

### 검증 커버리지

| 카테고리 | 전체 | 검증 완료 | 제외 | 커버리지 |
|---------|------|-----------|------|----------|
| Authentication | 3 | 1 | 2 | 33.3% |
| User Management | 5 | 3 | 2 | 60.0% |
| Posts | 5 | 4 | 1 | 80.0% |
| Comments | 4 | 4 | 0 | 100% |
| Likes | 3 | 0 | 3 | 0% |
| Images | 1 | 1 | 0 | 100% |
| **합계** | **21** | **13** | **8** | **61.9%** |

---

## 검증 완료 테스트 케이스 (QA 양식)

### 1. Authentication (인증) - 1개

| Process | No | Step | Test Case | Status | Expected result | Actual result | Comment |
|---------|----|----|-----------|--------|-----------------|---------------|---------|
| Authentication | 1 | 로그인 | POST /auth/login | Pass | HTTP 200 OK<br>- accessToken, refreshToken 발급<br>- localStorage 저장<br>- /board/list.html 리다이렉트 | HTTP 200 OK<br>- accessToken 길이: 185자<br>- refreshToken 길이: 127자<br>- localStorage 저장 성공<br>- 리다이렉트 성공 | 2025-10-18 검증 완료<br>커밋: a49ebcc (필드명 수정) |

---

### 2. User Management (사용자) - 3개

| Process | No | Step | Test Case | Status | Expected result | Actual result | Comment |
|---------|----|----|-----------|--------|-----------------|---------------|---------|
| User Management | 1 | 회원가입 | POST /users/signup | Pass | HTTP 201 Created<br>- accessToken, refreshToken 발급<br>- DB users 테이블 insert<br>- 프로필 이미지 S3 업로드 (선택)<br>- /board/list.html 리다이렉트 | HTTP 201 Created<br>- user_id=11 생성<br>- 토큰 정상 발급<br>- 프로필 이미지 업로드 성공 (선택 시)<br>- 자동 로그인 성공 | 2025-10-18 검증 완료<br>multipart/form-data 방식 |
| User Management | 2 | 프로필 조회 | GET /users/{id} | Pass | HTTP 200 OK<br>- userId, email, nickname, profileImage 필드 반환<br>- 프로필 이미지 URL 유효성 | HTTP 200 OK<br>- 모든 필드 정상 반환<br>- profileImage null (미설정 시) | 2025-10-18 검증 완료<br>공개 프로필 조회 |
| User Management | 3 | 비밀번호 변경 | PATCH /users/{id}/password | Pass | HTTP 200 OK<br>- 비밀번호 정책 검증 (8-20자, 대/소/특수)<br>- DB password_hash 업데이트<br>- 새 비밀번호로 재로그인 가능 | HTTP 200 OK<br>- 비밀번호 변경 성공<br>- 새 비밀번호로 로그인 확인 | 2025-10-18 검증 완료<br>커밋: d7c7b61 (필드명 수정) |

---

### 3. Posts (게시글) - 4개

| Process | No | Step | Test Case | Status | Expected result | Actual result | Comment |
|---------|----|----|-----------|--------|-----------------|---------------|---------|
| Posts | 1 | 목록 조회 | GET /posts | Pass | HTTP 200 OK<br>- posts 배열, nextCursor, hasMore 반환<br>- Cursor 페이지네이션 동작<br>- 게시글 렌더링 (제목, 작성자, 날짜, 통계) | HTTP 200 OK<br>- Cursor 페이지네이션 정상<br>- nextCursor=null, hasMore=false (데이터 부족)<br>- 게시글 카드 렌더링 성공 | 2025-10-18 검증 완료<br>latest 정렬 (Cursor) |
| Posts | 2 | 게시글 작성 | POST /posts | Pass | HTTP 201 Created<br>- postId 반환<br>- DB posts, post_stats 테이블 insert<br>- 이미지 연결 (post_images)<br>- 이미지 TTL 해제 (expires_at → NULL) | HTTP 201 Created<br>- post_id=8 생성<br>- post_stats 초기화 (0/0/0) 확인<br>- 이미지 TTL 해제 확인 | 2025-10-18 검증 완료<br>커밋: db3b42f (필드명 수정) |
| Posts | 3 | 게시글 수정 | PATCH /posts/{id} | Pass | HTTP 200 OK<br>- 제목/내용 변경 확인<br>- 이미지 변경 확인<br>- updatedAt 시간 업데이트<br>- 작성자만 수정 가능 (403 권한 검증) | HTTP 200 OK<br>- 모든 필드 정상 수정<br>- updatedAt 시간 변경 확인<br>- 권한 검증 정상 | 2025-10-18 검증 완료<br>PATCH 부분 업데이트 |
| Posts | 4 | 게시글 삭제 | DELETE /posts/{id} | Pass | HTTP 204 No Content<br>- DB post_status = DELETED (Soft Delete)<br>- /board/list.html 리다이렉트<br>- 목록에서 제거 확인 | HTTP 204 No Content<br>- Soft Delete 확인<br>- 리다이렉트 성공<br>- 목록에서 제거됨 | 2025-10-18 검증 완료<br>Soft Delete 정책 |

---

### 4. Comments (댓글) - 4개

| Process | No | Step | Test Case | Status | Expected result | Actual result | Comment |
|---------|----|----|-----------|--------|-----------------|---------------|---------|
| Comments | 1 | 목록 조회 | GET /posts/{postId}/comments | Pass | HTTP 200 OK<br>- comments 배열 반환<br>- pagination.totalCount 존재<br>- 댓글 렌더링 | HTTP 200 OK<br>- Offset 페이지네이션 정상<br>- 작성일 오름차순 정렬 | 2025-10-18 검증 완료<br>Offset 방식 (limit=10) |
| Comments | 2 | 댓글 작성 | POST /posts/{postId}/comments | Pass | HTTP 201 Created<br>- commentId 반환<br>- 댓글 목록 최상단 추가<br>- commentCount +1 | HTTP 201 Created<br>- 실시간 댓글 추가 성공<br>- commentCount 정확 업데이트 | 2025-10-18 검증 완료<br>커밋: a933570 (off-by-one 수정) |
| Comments | 3 | 댓글 수정 | PATCH /posts/{postId}/comments/{commentId} | Pass | HTTP 200 OK<br>- 댓글 내용 즉시 업데이트<br>- updatedAt 시간 변경<br>- 작성자만 수정 가능 (403 권한 검증) | HTTP 200 OK<br>- 댓글 수정 성공<br>- updatedAt 시간 변경 확인 | 2025-10-18 검증 완료<br>권한 검증 정상 |
| Comments | 4 | 댓글 삭제 | DELETE /posts/{postId}/comments/{commentId} | Pass | HTTP 204 No Content<br>- 댓글 목록에서 제거<br>- commentCount -1<br>- DB comment_status = DELETED (Soft Delete) | HTTP 204 No Content<br>- 댓글 삭제 성공<br>- commentCount 정확 감소 | 2025-10-18 검증 완료<br>Soft Delete 정책 |

---

### 5. Images (이미지) - 1개

| Process | No | Step | Test Case | Status | Expected result | Actual result | Comment |
|---------|----|----|-----------|--------|-----------------|---------------|---------|
| Images | 1 | 이미지 업로드 | POST /images | Pass | HTTP 201 Created<br>- imageId, imageUrl 반환<br>- S3 업로드 확인<br>- DB images 테이블 insert<br>- expires_at = NOW() + 1시간 | HTTP 201 Created<br>- S3 업로드 성공<br>- TTL 설정 확인 (1시간)<br>- 파일 검증 (JPG/PNG/GIF, 5MB 이하) | 2025-10-17 검증 완료<br>커밋: de7aea8 (필드명 수정) |

---

## 제외된 테스트 케이스 (8개)

### 제외 사유 분류

| 사유 | 개수 |
|------|------|
| **미테스트** | 2개 |
| **부분 검증 (에러 처리 미검증)** | 3개 |
| **부분 검증 (청소 로직 미검증)** | 1개 |
| **잘못된 정보 (메시지 부정확)** | 1개 |
| **부분 검증 (UI 미고지)** | 1개 |

---

### 상세 제외 사유

#### 1. TC-AUTH-002: POST /auth/logout ❌
**제외 사유**: 미테스트
**상태**: 테스트 우선순위 낮음으로 진행하지 않음
**영향**: 낮음 (로그인/회원가입 정상 작동 확인 우선)

---

#### 2. TC-AUTH-003: POST /auth/refresh_token ⚠️
**제외 사유**: 부분 검증 (자동 처리만 확인)
**검증됨**: fetchWithAuth 내부에서 401 응답 시 자동 호출 확인
**미검증**: 명시적인 API 테스트 없음, 엣지 케이스 미확인
**영향**: 중간 (토큰 갱신 자체는 동작하지만 완전 검증 아님)

---

#### 3. TC-USER-003: PATCH /users/{id} ❌
**제외 사유**: 부분 검증 (프로필 이미지 교체 시 구 이미지 누수)
**검증됨**:
- ✅ HTTP 200 OK
- ✅ 닉네임 변경 확인
- ✅ 새 프로필 이미지 업로드 및 변경 확인

**미검증**:
- ❌ 기존 프로필 이미지 S3 삭제 확인
- ❌ 기존 이미지 DB 레코드 정리
- ❌ 메모리 누수 방지 검증

**실제 문제**:
```java
// UserService.java:75-82
if (request.getProfileImage() != null) {
    ImageResponse imageResponse = imageService.uploadImage(request.getProfileImage());
    Image image = imageRepository.findById(imageResponse.getImageId())
            .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

    image.clearExpiresAt();  // 새 이미지는 영구 보존
    user.updateProfileImage(image);  // 새 이미지로 교체
    // ❌ 기존 이미지는 삭제하지 않음!
}
```

**장기적 영향**:
- S3 스토리지 비용 증가 (고아 이미지 누적)
- DB 이미지 테이블 비대화
- GDPR Right to Erasure 위반 가능성

**우선순위**: P1 High

---

#### 4. TC-USER-005: PUT /users/{id} ❌
**제외 사유**: 잘못된 정보 (사용자 오인 유도)
**검증됨**:
- ✅ HTTP 200 OK
- ✅ DB user_status = INACTIVE
- ✅ localStorage 토큰 삭제
- ✅ /user/login.html 리다이렉트

**미검증**:
- ❌ 프론트엔드 메시지 정확성 검증
- ❌ 실제 데이터 삭제 여부 확인
- ❌ GDPR 준수 검증

**실제 문제**:
```javascript
// profile-edit.js:164
if (!confirm('정말 탈퇴하시겠습니까? 모든 데이터가 삭제됩니다.')) return;
//                                   ^^^^^^^^^^^^^^^^^ 거짓 주장!
if (!confirm('탈퇴 후에는 복구할 수 없습니다. 계속하시겠습니까?')) return;
```

**백엔드 실제 동작**:
```java
// UserService.java:148
user.updateStatus(UserStatus.INACTIVE);  // Soft Delete만 수행
```

**결과**: 사용자에게 "모든 데이터가 삭제됩니다"라고 안내하지만, 실제로는:
- 게시글 유지 (user_id 외래 키 RESTRICT)
- 댓글 유지 (user_id 외래 키 RESTRICT)
- 좋아요 내역 유지
- 프로필 이미지 유지 (S3 + DB)
- 사용자 정보 유지 (status=INACTIVE만 변경)

**영향**: GDPR 위반 가능성, 사용자 신뢰 손상
**우선순위**: P0 Critical

---

#### 5. TC-POST-002: GET /posts/{id} ⚠️
**제외 사유**: 부분 검증 (다중 탭 불일치 미고지)
**검증됨**:
- ✅ HTTP 200 OK
- ✅ Optimistic Update 동작 확인 (viewCount +1)
- ✅ F5 새로고침 시 동기화

**미검증**:
- ❌ 다중 탭 환경에서의 일시적 불일치 사용자 고지
- ❌ UI에 stale 데이터임을 알리는 표시

**실제 구현**:
```javascript
// detail.js:483-505 (주석으로만 설명)
// [주의사항]
// - 다중 탭 동시 접속 시 네트워크 지연으로 일시적 불일치 가능
//   예) 탭2 먼저 도착(102) → 탭1 늦게 도착(101) → 탭1에 101 표시
// - F5 새로고침 시 정확한 값으로 자연스럽게 동기화
```

**문제**: 주석에만 설명되어 있고, 사용자에게는 알리지 않음
**우선순위**: P3 Low

---

#### 6. TC-LIKE-001: POST /posts/{id}/like ❌
**제외 사유**: 부분 검증 (Optimistic Update 에러 처리 미검증)
**검증됨**:
- ✅ HTTP 200 OK
- ✅ UI 즉시 업데이트 (Optimistic Update)
- ✅ likeCount +1
- ✅ 버튼 스타일 변경

**미검증**:
- ❌ API 실패 시 UI Rollback 동작 검증
- ❌ 네트워크 오류 시나리오 테스트
- ❌ 409 Conflict 에러 시 복원 확인
- ❌ showError() 메시지 표시 확인

**구현된 Rollback 코드**:
```javascript
// detail.js:222-229
catch (error) {
  console.error('Failed to toggle like:', error);
  // Rollback: 원래 상태로 복원
  state.isLiked = originalLiked;
  updateLikeButton(originalLiked);
  updateLikeCount(originalCount);
  showError(error.message);  // ← 이 부분이 실제로 작동하는지 미검증
}
```

**잠재적 문제**:
- 에러 발생 시 사용자 피드백이 제대로 전달되는지 미확인
- Rollback 로직이 복잡한 상황(다중 탭, 네트워크 재연결)에서도 정확한지 미검증

**우선순위**: P1 High

---

#### 7. TC-LIKE-002: DELETE /posts/{id}/like ❌
**제외 사유**: 부분 검증 (TC-LIKE-001과 동일한 에러 처리 미검증)
**검증/미검증 항목**: TC-LIKE-001과 동일
**우선순위**: P1 High

---

#### 8. TC-LIKE-003: GET /posts/users/me/likes ❌
**제외 사유**: 미테스트
**상태**: UI 미구현 (좋아요 목록 페이지 없음)
**영향**: 중간 (기능 자체는 구현되었으나 프론트엔드 미완성)

---

## 테스트 통계

### 카테고리별 상세

| 카테고리 | API | 검증 완료 | 제외 | 커버리지 | 비고 |
|---------|-----|-----------|------|----------|------|
| **Authentication** | POST /auth/login | ✅ Pass | - | 33.3% (1/3) | 로그인만 완전 검증 |
| Authentication | POST /auth/logout | - | ❌ 미테스트 | - | 우선순위 낮음 |
| Authentication | POST /auth/refresh_token | - | ⚠️ 부분 검증 | - | 자동 처리만 확인 |
| **User Management** | POST /users/signup | ✅ Pass | - | 60.0% (3/5) | Multipart 방식 |
| User Management | GET /users/{id} | ✅ Pass | - | - | 공개 프로필 |
| User Management | PATCH /users/{id} | - | ❌ 부분 검증 | - | 이미지 누수 (P1) |
| User Management | PATCH /users/{id}/password | ✅ Pass | - | - | 비밀번호 정책 검증 |
| User Management | PUT /users/{id} | - | ❌ 잘못된 정보 | - | 메시지 부정확 (P0) |
| **Posts** | GET /posts | ✅ Pass | - | 80.0% (4/5) | Cursor 페이지네이션 |
| Posts | GET /posts/{id} | - | ⚠️ 부분 검증 | - | 다중 탭 미고지 (P3) |
| Posts | POST /posts | ✅ Pass | - | - | 이미지 TTL 해제 |
| Posts | PATCH /posts/{id} | ✅ Pass | - | - | 권한 검증 |
| Posts | DELETE /posts/{id} | ✅ Pass | - | - | Soft Delete |
| **Comments** | GET /posts/{postId}/comments | ✅ Pass | - | 100% (4/4) | Offset 방식 |
| Comments | POST /posts/{postId}/comments | ✅ Pass | - | - | 실시간 추가 |
| Comments | PATCH /posts/{postId}/comments/{commentId} | ✅ Pass | - | - | 권한 검증 |
| Comments | DELETE /posts/{postId}/comments/{commentId} | ✅ Pass | - | - | Soft Delete |
| **Likes** | POST /posts/{id}/like | - | ❌ 부분 검증 | 0% (0/3) | 에러 처리 미검증 (P1) |
| Likes | DELETE /posts/{id}/like | - | ❌ 부분 검증 | - | 에러 처리 미검증 (P1) |
| Likes | GET /posts/users/me/likes | - | ❌ 미테스트 | - | UI 미구현 |
| **Images** | POST /images | ✅ Pass | - | 100% (1/1) | S3 직접 저장 |

---

## 버그 수정 이력 (검증 과정에서 발견)

| 버그 번호 | 심각도 | 내용 | 수정 커밋 | 상태 |
|----------|--------|------|-----------|------|
| Bug-1 | Critical | JWT 토큰 localStorage 저장 실패 (필드명 불일치: snake_case → camelCase) | a49ebcc | ✅ 수정 완료 |
| Bug-2 | Critical | "Entity not managed" 백엔드 버그 (Optimistic Update 패턴 도입) | 8103318 | ✅ 수정 완료 |
| Bug-3 | High | 이미지 업로드 필드명 불일치 (image_id → imageId) | de7aea8, db3b42f | ✅ 수정 완료 |
| Bug-4 | Medium | 비밀번호 변경 필드명 불일치 (new_password → newPassword) | d7c7b61 | ✅ 수정 완료 |
| Bug-5 | Low | 댓글 카운트 off-by-one (state.comments.length ± 1 중복 계산) | a933570 | ✅ 수정 완료 |

---

## 권장 조치사항

### 즉시 수정 필요 (P0-P1)

#### 1. TC-USER-005: 회원 탈퇴 메시지 수정 (P0)
```javascript
// Before (거짓)
'정말 탈퇴하시겠습니까? 모든 데이터가 삭제됩니다.'

// After (정확)
'정말 탈퇴하시겠습니까? 계정은 비활성화되며, 작성한 게시글과 댓글은 유지됩니다.'
```

#### 2. TC-USER-003: 프로필 이미지 교체 시 구 이미지 삭제 (P1)
```java
// UserService.updateProfile() 수정 필요
Image oldImage = user.getImage();
user.updateProfileImage(newImage);
if (oldImage != null) {
    imageService.deleteImage(oldImage.getImageId());  // S3 + DB 삭제 추가
}
```

#### 3. TC-LIKE-001/002: Optimistic Update 에러 테스트 추가 (P1)
- 네트워크 오류 시 Rollback 검증
- 409 Conflict 에러 시 Rollback 검증
- showError() 메시지 표시 확인
- 다중 탭 환경에서의 동작 확인

---

### 추후 검증 필요

#### 4. TC-AUTH-002/003: 로그아웃 및 토큰 갱신 명시적 테스트
- 로그아웃 API 직접 호출 테스트
- 토큰 갱신 엣지 케이스 검증

#### 5. TC-LIKE-003: 좋아요 목록 UI 구현 후 재테스트
- 프론트엔드 UI 완성 후 통합 테스트

#### 6. Phase 4 배치 작업 실행 검증
- 고아 이미지 정리 배치 작업 실제 실행 확인
- S3 파일 삭제 및 DB 레코드 정리 검증

---

## 참조 문서

- `docs/test/result/_legacy/FULL_TEST_REPORT.md` - 전체 테스트 리포트 (Legacy)
- `docs/test/result/_legacy/API_TEST_CASES.md` - API 테스트 케이스 상세 명세 (Legacy)
- `docs/test/result/_legacy/BUG_REPORT.md` - 버그 리포트 (Legacy)
- `docs/be/API.md` - REST API 명세
- `docs/be/LLD.md` - 백엔드 아키텍처
- `docs/be/DDL.md` - 데이터베이스 스키마

---

**최종 수정**: 2025-10-19
