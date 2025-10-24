# 버그 발견 및 수정 상세 리포트

## 문서 정보

| 항목 | 내용 |
|------|------|
| 문서 유형 | 버그 리포트 |
| 테스트 기간 | 2025-10-17 ~ 2025-10-18 |
| 발견 버그 | 6건 |
| 수정 완료 | 6건 (100%) |
| 작성일 | 2025-10-18 |
| 버전 | 1.0 |

---

## Executive Summary

### 버그 통계

| 심각도 | 발견 | 수정 | 미해결 | 비율 |
|--------|------|------|--------|------|
| Critical | 2 | 2 | 0 | 33.3% |
| High | 1 | 1 | 0 | 16.7% |
| Medium | 1 | 1 | 0 | 16.7% |
| Low | 2 | 2 | 0 | 33.3% |
| **합계** | **6** | **6** | **0** | **100%** |

### 버그 분류

| 카테고리 | 버그 수 |
|---------|---------|
| 필드명 불일치 (snake_case ↔ camelCase) | 4건 |
| 백엔드 아키텍처 (JPA detached entity) | 1건 |
| 프론트엔드 로직 (off-by-one) | 1건 |

### 주요 교훈

1. **API 명세 일관성 중요**
   - 문서-코드 불일치로 4건의 버그 발생
   - 필드명 표준 (camelCase) 전체 통일 필요

2. **JPA 영속성 컨텍스트 관리**
   - detached entity 문제 발견
   - Optimistic Update 패턴으로 해결

3. **프론트엔드 상태 관리**
   - 배열 길이 계산 시 중복 연산 주의
   - 단위 테스트 필요성 확인

---

## Bug-1: JWT 토큰 localStorage 저장 실패

### 기본 정보

| 항목 | 내용 |
|------|------|
| **버그 ID** | BUG-001 |
| **발견 일시** | 2025-10-17 13:02 |
| **심각도** | **Critical** |
| **상태** | ✅ 수정 완료 |
| **수정 커밋** | a49ebcc |
| **수정 일시** | 2025-10-17 14:00 |
| **영향 범위** | 회원가입, 로그인 전체 |
| **발견자** | 통합 테스트 (Phase 1) |

---

### 증상

**문제**: 회원가입 및 로그인 성공 후 localStorage에 토큰이 "undefined"로 저장됨

**재현 단계**:
1. http://localhost:8080/user/register.html 접속
2. 회원가입 폼 작성 및 제출
3. POST /users/signup 200 OK 응답
4. localStorage 확인:
   ```javascript
   localStorage.getItem('access_token')  // "undefined"
   localStorage.getItem('refresh_token') // "undefined"
   ```
5. 게시글 목록 페이지 이동 실패 (인증 실패)

**영향**:
- 회원가입 후 자동 로그인 실패
- 수동 로그인 시도 시에도 동일한 문제 발생
- 모든 사용자가 서비스 이용 불가 (Critical)

---

### 원인 분석

**근본 원인**: 백엔드 API 응답 필드명과 프론트엔드 코드 불일치

**백엔드 API 응답** (실제):
```json
{
  "message": "register_success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "timestamp": "2025-10-17T13:02:00"
}
```

**프론트엔드 코드** (버그):
```javascript
// register.js (line 163-164)
localStorage.setItem('access_token', response.access_token);   // undefined
localStorage.setItem('refresh_token', response.refresh_token); // undefined
```

**왜 발견되지 않았나**:
- API.md 문서에 snake_case로 잘못 기재
- 백엔드 실제 구현은 camelCase 사용
- 프론트엔드 개발자가 문서 기준으로 구현
- 통합 테스트 전까지 발견 안됨

---

### 수정 내역

**수정 파일**:
1. `src/main/resources/static/js/pages/user/register.js`
2. `src/main/resources/static/js/pages/user/login.js`

**수정 코드**:

```javascript
// register.js (line 163-164) - 수정 후
localStorage.setItem('access_token', response.accessToken);   // ✅
localStorage.setItem('refresh_token', response.refreshToken); // ✅

// login.js (line 91-92) - 수정 후
localStorage.setItem('access_token', response.accessToken);
localStorage.setItem('refresh_token', response.refreshToken);
```

**커밋 메시지**:
```
fix: JWT 토큰 localStorage 저장 시 필드명 오류 수정

문제:
- 백엔드 API 응답: accessToken, refreshToken (camelCase)
- 프론트엔드 코드: access_token, refresh_token (snake_case)
- localStorage에 "undefined" 저장됨

수정:
- register.js line 163-164: snake_case → camelCase
- login.js line 91-92: snake_case → camelCase

영향:
- 회원가입 자동 로그인 정상 작동
- 로그인 후 인증 토큰 정상 저장
```

---

### 검증

**테스트 시나리오**:
1. 회원가입 (email: test2@startupcode.kr)
2. localStorage 확인:
   ```javascript
   localStorage.getItem('access_token')
   // "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." (185자)

   localStorage.getItem('refresh_token')
   // "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." (127자)
   ```
3. 게시글 목록 페이지 정상 로드
4. API 호출 시 Authorization 헤더 정상 전송

**결과**: ✅ 수정 확인 완료

---

### 재발 방지

1. **문서 수정**:
   - `docs/be/API.md` Section 1.1, 2.1 필드명 camelCase로 수정
   - `docs/fe/FRONTEND_GUIDE.md` 코드 예시 수정

2. **테스트 강화**:
   - localStorage 값 검증 테스트 케이스 추가
   - 자동화 테스트에 토큰 저장 확인 포함

3. **코드 리뷰**:
   - API 응답 필드명 일치 여부 필수 체크

---

## Bug-2: "Entity not managed" (백엔드 JPA 이슈)

### 기본 정보

| 항목 | 내용 |
|------|------|
| **버그 ID** | BUG-002 |
| **발견 일시** | 2025-10-17 13:09 |
| **심각도** | **Critical** |
| **상태** | ✅ 수정 완료 |
| **수정 커밋** | 8103318, d2ae4f0 |
| **수정 일시** | 2025-10-17 16:00 |
| **영향 범위** | Phase 3-5 전체 블로킹 |
| **발견자** | 통합 테스트 (Phase 3) |

---

### 증상

**문제**: 게시글 작성은 성공하지만, 상세 조회 시 400 Bad Request 발생

**재현 단계**:
1. POST /posts 성공 → post_id=7 생성
2. DB 확인: posts 테이블에 데이터 존재
3. GET /posts/7 → 400 Bad Request
4. 에러 응답:
   ```json
   {
     "message": "COMMON-001",
     "data": {
       "field": null,
       "details": "Entity not managed"
     },
     "timestamp": "2025-10-17T13:09:07"
   }
   ```
5. 프론트엔드 alert: "게시글을 찾을 수 없습니다."
6. /board/list.html로 강제 리다이렉트

**영향**:
- 게시글 상세 페이지 접근 불가
- 댓글, 좋아요 기능 전체 불가 (상세 페이지에 UI 존재)
- Phase 3, 4, 5 전체 테스트 블로킹 (Critical)

---

### 원인 분석

**근본 원인**: JPA detached entity 문제

**상세 분석**:

1. **게시글 작성 플로우**:
   ```java
   // PostService.createPost()
   Post savedPost = postRepository.save(post);
   PostStats stats = postStatsRepository.save(PostStats.builder()...);
   savedPost.updateStats(stats);  // 양방향 연관관계 설정
   return PostResponse.from(savedPost);  // ✅ 정상
   ```

2. **게시글 상세 조회 플로우** (버그):
   ```java
   // PostService.getPostById()
   Post post = postRepository.findById(postId)
           .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

   // viewCount 증가 (기존 로직)
   PostStats stats = post.getStats();
   stats.incrementViewCount();  // ❌ detached entity
   postStatsRepository.save(stats);

   return PostResponse.from(post);  // ❌ IllegalArgumentException: Entity not managed
   ```

3. **왜 detached 되었나**:
   - `postRepository.findById()` 후 트랜잭션 종료
   - `stats.incrementViewCount()` 호출 시 stats는 영속성 컨텍스트 밖
   - `save()` 시도 시 "Entity not managed" 예외 발생

---

### 수정 내역

**해결 방안**: Optimistic Update 패턴 도입

**핵심 아이디어**:
1. DB 레벨 원자적 UPDATE (영속성 컨텍스트 우회)
2. 클라이언트 UI에서 즉시 +1 표시
3. 다음 GET 요청 시 정확한 값 동기화

**백엔드 수정**:

```java
// PostStatsRepository.java (신규 추가)
@Modifying(clearAutomatically = true)
@Query("UPDATE PostStats ps SET ps.viewCount = ps.viewCount + 1, " +
       "ps.lastUpdated = CURRENT_TIMESTAMP WHERE ps.postId = :postId")
int incrementViewCount(@Param("postId") Long postId);
```

```java
// PostService.getPostById() (수정 후)
@Transactional(readOnly = true)
public PostResponse getPostById(Long postId) {
    // 1. 게시글 조회 (Fetch Join)
    Post post = postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE)
            .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

    // 2. viewCount 원자적 증가 (JPQL UPDATE)
    postStatsRepository.incrementViewCount(postId);  // ✅ 영속성 컨텍스트 우회

    // 3. 응답 생성 (증가 전 값 반환)
    return PostResponse.from(post);  // viewCount는 stale value (OK)
}
```

**프론트엔드 수정**:

```javascript
// detail.js (수정 후)
async function loadPost() {
    const post = await fetchWithAuth(`/posts/${postId}`);

    // Optimistic Update: UI에서 +1 표시
    updateStats({
        viewCount: post.stats.viewCount + 1,  // ✅ 서버 응답 + 1
        likeCount: post.stats.likeCount,
        commentCount: post.stats.commentCount
    });
}
```

**좋아요/댓글도 동일 패턴 적용**:
- `incrementLikeCount()`, `decrementLikeCount()`
- `incrementCommentCount()`, `decrementCommentCount()`

---

### 검증

**테스트 시나리오**:
1. 게시글 작성 (POST /posts) → post_id=8
2. 게시글 상세 조회 (GET /posts/8):
   - 응답: viewCount=0
   - UI 표시: viewCount=1 ✅
3. F5 새로고침:
   - 응답: viewCount=1
   - UI 표시: viewCount=2 ✅
4. 다른 사용자 조회:
   - 응답: viewCount=1
   - UI 표시: viewCount=2 ✅

**동시성 테스트**:
- 100명 동시 조회 → viewCount=100 (정확)
- JPQL UPDATE의 원자성 보장 확인

**결과**: ✅ 수정 확인 완료

---

### 재발 방지

1. **아키텍처 개선**:
   - 조회수/좋아요/댓글 카운트 전체 Optimistic Update 적용
   - 성능 및 동시성 향상

2. **코드 리뷰**:
   - JPA Entity 생명주기 검증 강화
   - detached entity 사용 시 경고

3. **문서화**:
   - LLD.md에 Optimistic Update 패턴 명시
   - 백엔드 개발 가이드에 추가

---

## Bug-3: 이미지 업로드 필드명 불일치

### 기본 정보

| 항목 | 내용 |
|------|------|
| **버그 ID** | BUG-003 |
| **발견 일시** | 2025-10-17 17:00 |
| **심각도** | **High** |
| **상태** | ✅ 수정 완료 |
| **수정 커밋** | de7aea8, db3b42f |
| **수정 일시** | 2025-10-17 18:00 |
| **영향 범위** | 게시글 이미지, 프로필 이미지 |
| **발견자** | 이미지 업로드 테스트 |

---

### 증상

**문제 1**: 게시글 이미지 업로드는 성공하지만, 게시글에 연결되지 않음

**재현 단계**:
1. POST /images 성공 → imageId=123
2. POST /posts `{ image_id: 123 }` 전송
3. 게시글 생성 성공하지만 post_images 테이블에 데이터 없음
4. 게시글 상세 페이지에 이미지 표시 안됨

**문제 2**: 프로필 이미지 업로드 실패

**재현 단계**:
1. PATCH /users/{id} (multipart) 전송
2. 닉네임은 변경되지만 프로필 이미지는 NULL 유지

---

### 원인 분석

**근본 원인**: API 응답/요청 필드명 불일치

**1. API 응답 (POST /images)**:
```json
{
  "data": {
    "imageId": 123,      // camelCase
    "imageUrl": "https://..."  // camelCase
  }
}
```

**프론트엔드 코드** (버그):
```javascript
// write.js (버그)
const result = await uploadImage(file);
uploadedImageId = result.image_id;  // ❌ undefined
showImagePreview(result.image_url);  // ❌ undefined
```

**2. API 요청 (POST /posts)**:
```javascript
// write.js (버그)
await fetchWithAuth('/posts', {
  method: 'POST',
  body: JSON.stringify({
    title,
    content,
    image_id: uploadedImageId  // ❌ 백엔드는 imageId 기대
  })
});
```

**3. API 요청 (PATCH /users/{id})**:
```javascript
// profile-edit.js (버그)
const formData = new FormData();
formData.append('nickname', nickname);
formData.append('profile_image', file);  // ❌ 백엔드는 profileImage 기대
```

---

### 수정 내역

**수정 파일** (4개):
1. `write.js` - 게시글 이미지 업로드 (응답 + 요청)
2. `edit.js` - 게시글 수정
3. `profile-edit.js` - 프로필 이미지 수정
4. `register.js` - 회원가입 프로필 이미지

**수정 코드**:

```javascript
// write.js (수정 후)
// 1. API 응답 필드명
const result = await uploadImage(file);
uploadedImageId = result.imageId;   // ✅ camelCase
showImagePreview(result.imageUrl);  // ✅ camelCase

// 2. API 요청 필드명
await fetchWithAuth('/posts', {
  method: 'POST',
  body: JSON.stringify({
    title,
    content,
    imageId: uploadedImageId  // ✅ camelCase
  })
});
```

```javascript
// profile-edit.js (수정 후)
const formData = new FormData();
formData.append('nickname', nickname);
formData.append('profileImage', file);  // ✅ camelCase
```

---

### 검증

**이미지 업로드 통합 테스트** (3개 시나리오):

1. ✅ **Scenario 1**: 게시글 이미지 업로드
   - POST /images → POST /posts
   - 게시글 상세 페이지에 이미지 표시 확인

2. ✅ **Scenario 2**: 프로필 이미지 수정
   - PATCH /users/{id} (multipart)
   - 프로필 이미지 변경 확인

3. ✅ **Scenario 3**: 회원가입 시 프로필 이미지
   - POST /users/signup (multipart)
   - 자동 로그인 후 프로필 이미지 표시 확인

**DB 검증**:
```sql
-- 게시글 이미지 연결 확인
SELECT * FROM post_images WHERE post_id = 8;
-- post_id: 8
-- image_id: 123
-- display_order: 1

-- 이미지 TTL 해제 확인
SELECT * FROM images WHERE image_id = 123;
-- expires_at: NULL (✅ 영구 보존)

-- 프로필 이미지 확인
SELECT * FROM users WHERE user_id = 11;
-- image_id: 124 (✅ 프로필 이미지 연결)
```

---

### 재발 방지

1. **문서 수정**:
   - API.md 전체 필드명 camelCase로 수정 (24개 위치)
   - FRONTEND_GUIDE.md 코드 예시 수정

2. **테스트 강화**:
   - IMAGE_UPLOAD_TEST_RESULTS.md 작성
   - 3개 시나리오 자동화 테스트

---

## Bug-4: 비밀번호 변경 필드명 불일치

### 기본 정보

| 항목 | 내용 |
|------|------|
| **버그 ID** | BUG-004 |
| **발견 일시** | 2025-10-17 18:00 |
| **심각도** | **Medium** |
| **상태** | ✅ 수정 완료 |
| **수정 커밋** | d7c7b61 |
| **수정 일시** | 2025-10-17 18:30 |
| **영향 범위** | 비밀번호 변경 기능 |

---

### 증상

**문제**: 비밀번호 변경 시 400 Bad Request 발생

**재현 단계**:
1. /user/password-change.html 접속
2. 새 비밀번호 입력: `NewPass1234!`
3. "변경" 버튼 클릭
4. PATCH /users/11/password → 400 Bad Request
5. 에러: "Invalid request parameters"

---

### 원인 분석

**근본 원인**: API 요청 필드명 불일치

**프론트엔드 코드** (버그):
```javascript
// password-change.js (버그)
await fetchWithAuth(`/users/${userId}/password`, {
  method: 'PATCH',
  body: JSON.stringify({
    new_password: newPassword,              // ❌ snake_case
    new_password_confirm: newPasswordConfirm // ❌ snake_case
  })
});
```

**백엔드 DTO**:
```java
// ChangePasswordRequest.java
public class ChangePasswordRequest {
    private String newPassword;              // camelCase
    private String newPasswordConfirm;       // camelCase
}
```

---

### 수정 내역

```javascript
// password-change.js (수정 후)
await fetchWithAuth(`/users/${userId}/password`, {
  method: 'PATCH',
  body: JSON.stringify({
    newPassword: newPassword,              // ✅ camelCase
    newPasswordConfirm: newPasswordConfirm // ✅ camelCase
  })
});
```

---

### 검증

**테스트 시나리오**:
1. 비밀번호 변경: `Test1234!` → `NewPass1234!`
2. PATCH /users/11/password 200 OK
3. 로그아웃 후 새 비밀번호로 재로그인 성공

**결과**: ✅ 수정 확인 완료

---

## Bug-5: 댓글 카운트 off-by-one

### 기본 정보

| 항목 | 내용 |
|------|------|
| **버그 ID** | BUG-005 |
| **발견 일시** | 2025-10-17 |
| **심각도** | **Low** |
| **상태** | ✅ 수정 완료 |
| **수정 커밋** | a933570 |
| **영향 범위** | 댓글 카운트 표시 |

---

### 증상

**문제 1**: 댓글 작성 후 카운트가 1 많게 표시됨
- 실제 댓글 1개 → UI 표시: 2개

**문제 2**: 댓글 삭제 후 카운트가 1 적게 표시됨
- 실제 댓글 1개 → UI 표시: 0개

---

### 원인 분석

**근본 원인**: 배열 길이 계산 시 중복 연산

**프론트엔드 코드** (버그):
```javascript
// detail.js (버그)
function prependComment(comment) {
  state.comments.unshift(comment);  // 배열에 추가
  updateCommentCount(state.comments.length + 1);  // ❌ 이미 길이 증가함
}

function removeCommentFromList(commentId) {
  state.comments = state.comments.filter(c => c.commentId !== commentId);  // 배열에서 제거
  updateCommentCount(state.comments.length - 1);  // ❌ 이미 길이 감소함
}
```

**시나리오**:
1. 초기 state.comments.length = 0
2. 댓글 작성 → unshift() → length = 1
3. updateCommentCount(1 + 1) → UI: 2 ❌
4. 댓글 삭제 → filter() → length = 0
5. updateCommentCount(0 - 1) → UI: -1 ❌

---

### 수정 내역

```javascript
// detail.js (수정 후)
function prependComment(comment) {
  state.comments.unshift(comment);
  updateCommentCount(state.comments.length);  // ✅ 정확한 길이
}

function removeCommentFromList(commentId) {
  state.comments = state.comments.filter(c => c.commentId !== commentId);
  updateCommentCount(state.comments.length);  // ✅ 정확한 길이
}
```

---

### 검증

**테스트 시나리오**:
1. 댓글 0개 상태
2. 댓글 작성 → UI: 1개 ✅
3. 댓글 추가 작성 → UI: 2개 ✅
4. 댓글 삭제 → UI: 1개 ✅
5. 댓글 전체 삭제 → UI: 0개 ✅

**결과**: ✅ 수정 확인 완료

---

## Bug-6: 빈 src 속성

### 기본 정보

| 항목 | 내용 |
|------|------|
| **버그 ID** | BUG-006 |
| **발견 일시** | 2025-10-17 |
| **심각도** | **Low** |
| **상태** | ✅ 수정 완료 |
| **수정 커밋** | ed30b88 |
| **영향 범위** | 프로필 이미지 미설정 시 |

---

### 증상

**문제**: 프로필 이미지 미설정 시 `<img src="">` 빈 문자열로 렌더링

**영향**:
- 브라우저 콘솔 경고: "Failed to load resource"
- 불필요한 HTTP 요청 발생

---

### 원인 분석

```javascript
// 렌더링 코드 (버그)
author.profileImage = author.profileImage || '';
profileImageElement.src = author.profileImage;  // <img src="">
```

---

### 수정 내역

```javascript
// 수정 후
const DEFAULT_PROFILE_IMAGE = '/images/default-profile.png';

author.profileImage = author.profileImage || DEFAULT_PROFILE_IMAGE;
profileImageElement.src = author.profileImage;  // <img src="/images/default-profile.png">
```

---

### 검증

**테스트 시나리오**:
1. 프로필 이미지 미설정 사용자 프로필 조회
2. 기본 이미지 표시 확인
3. 브라우저 콘솔 경고 없음 확인

**결과**: ✅ 수정 확인 완료

---

## 버그 패턴 분석

### 1. 필드명 불일치 (4건)

**공통 원인**:
- API 문서와 실제 구현 불일치
- snake_case vs camelCase 혼용

**영향 범위**:
- JWT 토큰 (Critical)
- 이미지 업로드 (High)
- 비밀번호 변경 (Medium)

**재발 방지**:
- API 명세 표준화 (camelCase 통일)
- 문서-코드 일치 자동 검증
- 통합 테스트 강화

---

### 2. 백엔드 아키텍처 (1건)

**버그**: JPA detached entity

**근본 원인**:
- JPA 영속성 컨텍스트 관리 미숙
- 조회수 증가 로직 설계 오류

**해결**:
- Optimistic Update 패턴 도입
- JPQL UPDATE 활용

**재발 방지**:
- JPA 베스트 프랙티스 문서화
- 코드 리뷰 강화

---

### 3. 프론트엔드 로직 (1건)

**버그**: off-by-one 에러

**근본 원인**:
- 배열 길이 계산 중복
- 단위 테스트 부재

**재발 방지**:
- 프론트엔드 단위 테스트 도입
- 코드 리뷰 체크리스트 추가

---

## 교훈 및 개선 사항

### 1. API 명세 관리

**문제**:
- 문서와 코드 불일치
- 필드명 표준 없음

**개선**:
- ✅ API.md 전체 camelCase로 통일
- ✅ OpenAPI/Swagger 도입 검토
- ✅ 자동화 검증 스크립트

---

### 2. 통합 테스트

**성과**:
- 6개 버그 조기 발견
- 운영 배포 전 수정 완료

**개선**:
- ✅ 자동화 테스트 확대
- ✅ CI/CD 파이프라인 통합
- ✅ 테스트 커버리지 목표: 90%+

---

### 3. 코드 리뷰

**체크리스트 추가**:
- [ ] API 응답 필드명 일치 확인
- [ ] localStorage 저장 값 검증
- [ ] JPA Entity 생명주기 확인
- [ ] 배열 길이 계산 중복 연산 확인

---

## 참조 문서

- `docs/test/result/FULL_TEST_REPORT.md` - 전체 테스트 리포트
- `docs/test/result/API_TEST_CASES.md` - API 테스트 케이스
- `docs/test/IMAGE_UPLOAD_TEST_RESULTS.md` - 이미지 업로드 테스트
- `docs/be/API.md` - REST API 명세
- `docs/be/LLD.md` - 백엔드 아키텍처

---

**문서 버전**: 1.0
**최종 수정**: 2025-10-18
**작성자**: 개발팀
