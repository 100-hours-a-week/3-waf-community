# 프론트엔드-백엔드 전체 연동 테스트 최종 리포트

## 문서 정보

| 항목 | 내용 |
|------|------|
| 테스트 기간 | 2025-10-17 ~ 2025-10-18 |
| 테스트 범위 | Phase 1-5 프론트엔드-백엔드 연동 + 이미지 업로드 |
| 테스트 도구 | MCP Chrome DevTools (브라우저 자동화) |
| 테스트 환경 | macOS, Chrome, MySQL 8.0, Spring Boot 3.5.6 |
| 버전 | 1.0 |

---

## Executive Summary

### 테스트 결과 요약

| 지표 | 값 |
|------|-----|
| **전체 API** | 21개 |
| **테스트 완료** | 18개 |
| **통과율** | **85.7%** |
| **발견 버그** | 6건 (전체 수정 완료) |
| **테스트 시나리오** | 21개 (5개 Phase) |
| **증빙 자료** | 스크린샷 9개, 리포트 3개 |

---

## 테스트 일정 및 이력

### Timeline

```
2025-10-17 13:02 - Phase 1 회원가입 테스트 시작
2025-10-17 13:08 - Phase 2 게시글 작성 완료
2025-10-17 13:09 - Phase 3 게시글 상세 조회 실패 (백엔드 버그 발견)
2025-10-17 15:06 - 백엔드 버그 분석 완료
2025-10-17 15:30 - Optimistic Update 패턴 도입 결정
2025-10-17 16:00 - 백엔드 수정 완료 (커밋 8103318)
2025-10-17 17:00 - 이미지 업로드 테스트 시작
2025-10-17 18:30 - 이미지 업로드 버그 수정 (커밋 de7aea8, db3b42f)
2025-10-18 17:15 - Phase 1-4 재테스트 시작
2025-10-18 18:06 - Phase 1-4 전체 통과, 스크린샷 캡처 완료
```

### 테스트 반복 이력

| 회차 | 날짜 | 범위 | 결과 | 비고 |
|------|------|------|------|------|
| 1차 | 2025-10-17 13:02 | Phase 1-3 | 부분 성공 | Phase 3 백엔드 버그 |
| 백엔드 수정 | 2025-10-17 16:00 | - | - | Optimistic Update 패턴 |
| 이미지 테스트 | 2025-10-17 17:00 | 3 시나리오 | 성공 | 필드명 버그 수정 |
| 2차 | 2025-10-18 17:15 | Phase 1-4 | 전체 성공 | 9개 스크린샷 캡처 |

---

## Phase별 상세 테스트 결과

## Phase 1: 인증 (Authentication)

### 테스트 목표
- 회원가입 및 자동 로그인 기능 검증
- JWT 토큰 발급 및 localStorage 저장 확인

### 테스트 시나리오

#### TC-1.1: 회원가입 (POST /users/signup)

**사전 조건**:
- 서버 실행 중 (http://localhost:8080)
- MySQL 데이터베이스 실행 중
- 테스트 이메일 미사용 상태

**테스트 단계**:
1. http://localhost:8080/user/register.html 접속
2. 폼 입력:
   - 이메일: `test@startupcode.kr`
   - 비밀번호: `Test1234!`
   - 닉네임: `테스터`
   - 프로필 이미지: 선택 (선택 사항)
3. "회원가입" 버튼 클릭

**예상 결과**:
- POST /users/signup 201 Created
- 응답: `{ accessToken, refreshToken }`
- localStorage에 토큰 저장
- /board/list.html로 자동 리다이렉트

**실제 결과** (1차 테스트):
- ❌ localStorage에 "undefined" 저장됨

**버그 원인**:
```javascript
// register.js (버그)
localStorage.setItem('access_token', response.access_token);  // undefined
localStorage.setItem('refresh_token', response.refresh_token); // undefined

// 백엔드 응답 (실제)
{
  "data": {
    "accessToken": "eyJ...",  // camelCase
    "refreshToken": "eyJ..."  // camelCase
  }
}
```

**수정 내역** (커밋 a49ebcc):
```javascript
// register.js (수정)
localStorage.setItem('access_token', response.accessToken);
localStorage.setItem('refresh_token', response.refreshToken);
```

**실제 결과** (2차 테스트):
- ✅ POST /users/signup 201 Created
- ✅ localStorage.access_token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." (185자)
- ✅ localStorage.refresh_token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." (127자)
- ✅ /board/list.html 리다이렉트 성공

**DB 확인**:
```sql
SELECT * FROM users WHERE email = 'test@startupcode.kr';
-- user_id: 11
-- email: test@startupcode.kr
-- nickname: 테스터
-- role: USER
-- user_status: ACTIVE
```

**증빙 자료**:
- `test-phase1-register-page.png` - 회원가입 폼
- `test-phase1-filled-form.png` - 입력 완료 상태
- `test-phase1-success-board-list.png` - 성공 후 리다이렉트
- `20251018_171539/phase1-register-success-redirected.png` - 2차 테스트

---

#### TC-1.2: 로그인 (POST /auth/login)

**사전 조건**:
- 회원가입 완료 (user_id=11)
- localStorage 토큰 삭제 (로그아웃 상태 시뮬레이션)

**테스트 단계**:
1. http://localhost:8080/user/login.html 접속
2. 폼 입력:
   - 이메일: `test@startupcode.kr`
   - 비밀번호: `Test1234!`
3. "로그인" 버튼 클릭

**예상 결과**:
- POST /auth/login 200 OK
- 응답: `{ accessToken, refreshToken }`
- localStorage 토큰 저장
- /board/list.html로 리다이렉트

**실제 결과**:
- ✅ POST /auth/login 200 OK
- ✅ localStorage 토큰 저장 성공
- ✅ 리다이렉트 성공

**API 요청/응답 예시**:
```http
POST /auth/login HTTP/1.1
Content-Type: application/json

{
  "email": "test@startupcode.kr",
  "password": "Test1234!"
}

HTTP/1.1 200 OK
{
  "message": "login_success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "timestamp": "2025-10-18T17:15:00"
}
```

---

## Phase 2: 게시글 목록 및 상세

### TC-2.1: 게시글 목록 조회 (GET /posts)

**사전 조건**:
- 로그인 완료 (localStorage 토큰 존재)
- DB에 게시글 0개 이상

**테스트 단계**:
1. http://localhost:8080/board/list.html 접속
2. 게시글 목록 자동 로드 확인
3. "더보기" 버튼 클릭 (있을 경우)

**예상 결과**:
- GET /posts?cursor=null&limit=10&sort=latest 200 OK
- 응답: `{ posts: [...], nextCursor, hasMore }`
- 게시글 카드 렌더링 (제목, 작성자, 날짜, 통계)
- Cursor 페이지네이션 동작

**실제 결과**:
- ✅ GET /posts 200 OK
- ✅ posts 배열 렌더링 성공
- ✅ 각 게시글 정보 정확:
  - 제목 표시
  - 작성자 닉네임 + 프로필 이미지
  - 상대 시간 표시 ("방금 전", "1시간 전")
  - 통계: 좋아요/댓글/조회수 (formatNumberCompact: "1.2k")
- ✅ nextCursor: null (데이터 없음)
- ✅ hasMore: false

**API 응답 예시** (게시글 1개 있을 때):
```json
{
  "message": "get_posts_success",
  "data": {
    "posts": [
      {
        "postId": 7,
        "title": "Phase 2 테스트 게시글",
        "content": "프론트엔드-백엔드 연동 테스트를 위한 첫 번째 게시글입니다...",
        "createdAt": "2025-10-17T13:08:55",
        "updatedAt": "2025-10-17T13:08:55",
        "author": {
          "userId": 11,
          "nickname": "테스터",
          "profileImage": null
        },
        "stats": {
          "likeCount": 0,
          "commentCount": 0,
          "viewCount": 0
        }
      }
    ],
    "nextCursor": null,
    "hasMore": false
  },
  "timestamp": "2025-10-17T13:09:00"
}
```

---

### TC-2.2: 게시글 상세 조회 (GET /posts/{id})

**사전 조건**:
- 로그인 완료
- DB에 게시글 존재 (post_id=7)

**테스트 단계**:
1. 게시글 목록에서 게시글 클릭
2. /board/detail.html?id=7 이동
3. 게시글 상세 정보 렌더링 확인

**1차 테스트 결과** (2025-10-17 13:09):
- ❌ GET /posts/7 → 400 Bad Request
- ❌ 에러 코드: COMMON-001
- ❌ 에러 메시지: "Entity not managed"

**백엔드 로그**:
```
2025-10-17 13:09:07 [http-nio-8080-exec-5] WARN  c.k.c.e.GlobalExceptionHandler - [Error] 잘못된 인자: Entity not managed
```

**버그 분석**:
- **원인**: JPA detached entity 문제
- **증상**: 게시글은 DB에 존재하지만, 상세 조회 시 Entity가 영속성 컨텍스트에서 분리됨
- **영향**: Phase 3, 4, 5 전체 블로킹

**백엔드 수정** (커밋 8103318):
- Optimistic Update 패턴 도입
- viewCount 증가: JPQL UPDATE (영속성 컨텍스트 우회)
- 클라이언트 UI: 응답값 + 1 표시

**2차 테스트 결과** (2025-10-18 17:20):
- ✅ GET /posts/7 200 OK
- ✅ 게시글 상세 정보 렌더링 성공:
  - 제목, 내용
  - 작성자 정보 (닉네임, 프로필 이미지)
  - 통계 (좋아요/댓글/조회수)
  - 생성일/수정일
- ✅ viewCount Optimistic Update 동작:
  - 서버 응답: viewCount=0
  - UI 표시: viewCount+1=1

**API 응답 예시**:
```json
{
  "message": "get_post_detail_success",
  "data": {
    "postId": 7,
    "title": "Phase 2 테스트 게시글",
    "content": "프론트엔드-백엔드 연동 테스트를 위한 첫 번째 게시글입니다. JWT 인증이 정상 작동하고 있으며, 게시글 작성 API를 테스트합니다.",
    "author": {
      "userId": 11,
      "nickname": "테스터",
      "profileImage": null
    },
    "stats": {
      "viewCount": 0,
      "likeCount": 0,
      "commentCount": 0
    },
    "createdAt": "2025-10-17T13:08:55",
    "updatedAt": "2025-10-17T13:08:55"
  },
  "timestamp": "2025-10-18T17:20:00"
}
```

**증빙 자료**:
- `phase2_post_detail_page.png` - 게시글 상세 페이지

---

## Phase 3: 좋아요 및 댓글

### TC-3.1: 좋아요 추가 (POST /posts/{id}/like)

**사전 조건**:
- 로그인 완료
- 게시글 상세 페이지 접속 (post_id=7)
- 좋아요 누르지 않은 상태

**테스트 단계**:
1. "좋아요" 버튼 클릭
2. UI 즉시 업데이트 확인 (Optimistic Update)
3. API 응답 확인

**예상 결과**:
- POST /posts/7/like 200 OK
- UI: likeCount 즉시 +1
- 다음 페이지 로드 시 정확한 값 동기화

**실제 결과**:
- ✅ POST /posts/7/like 200 OK
- ✅ UI 즉시 업데이트: 0 → 1
- ✅ 버튼 스타일 변경 (비활성 → 활성)

**API 응답 예시** (Phase 5 이후):
```json
{
  "message": "like_success",
  "data": {
    "message": "like_success"
  },
  "timestamp": "2025-10-18T17:25:00"
}
```

**참고**: Phase 5 업데이트로 `likeCount` 응답 제거, Optimistic Update 패턴 적용

---

### TC-3.2: 좋아요 취소 (DELETE /posts/{id}/like)

**사전 조건**:
- 좋아요 이미 누른 상태

**테스트 단계**:
1. "좋아요" 버튼 다시 클릭
2. UI 즉시 업데이트 확인

**실제 결과**:
- ✅ DELETE /posts/7/like 200 OK
- ✅ UI 즉시 업데이트: 1 → 0
- ✅ 버튼 스타일 변경 (활성 → 비활성)

---

### TC-3.3: 댓글 작성 (POST /posts/{id}/comments)

**사전 조건**:
- 로그인 완료
- 게시글 상세 페이지 접속

**테스트 단계**:
1. 댓글 입력란에 텍스트 입력: "첫 번째 댓글입니다."
2. "댓글 작성" 버튼 클릭
3. 댓글 목록에 즉시 추가되는지 확인

**예상 결과**:
- POST /posts/7/comments 201 Created
- 응답: `{ commentId, content, author, createdAt }`
- 댓글 목록 최상단에 추가
- commentCount +1

**실제 결과**:
- ✅ POST /posts/7/comments 201 Created
- ✅ 댓글 목록에 즉시 추가
- ✅ commentCount 정확: 0 → 1

**버그 발견 및 수정** (커밋 a933570):
```javascript
// detail.js (버그)
function prependComment(comment) {
  state.comments.unshift(comment);
  updateCommentCount(state.comments.length + 1);  // ❌ off-by-one
}

// detail.js (수정)
function prependComment(comment) {
  state.comments.unshift(comment);
  updateCommentCount(state.comments.length);  // ✅ 정확한 카운트
}
```

**API 요청/응답 예시**:
```http
POST /posts/7/comments HTTP/1.1
Authorization: Bearer eyJ...
Content-Type: application/json

{
  "comment": "첫 번째 댓글입니다."
}

HTTP/1.1 201 Created
{
  "message": "create_comment_success",
  "data": {
    "commentId": 1,
    "content": "첫 번째 댓글입니다.",
    "author": {
      "userId": 11,
      "nickname": "테스터",
      "profileImage": null
    },
    "createdAt": "2025-10-18T17:30:00",
    "updatedAt": "2025-10-18T17:30:00"
  },
  "timestamp": "2025-10-18T17:30:00"
}
```

**증빙 자료**:
- `phase2_comment_created.png` - 댓글 작성 성공

---

### TC-3.4: 댓글 수정 (PATCH /posts/{postId}/comments/{commentId})

**사전 조건**:
- 본인이 작성한 댓글 존재

**테스트 단계**:
1. 댓글 "수정" 버튼 클릭
2. 댓글 내용 수정: "수정된 댓글입니다."
3. "저장" 버튼 클릭

**실제 결과**:
- ✅ PATCH /posts/7/comments/1 200 OK
- ✅ 댓글 내용 즉시 업데이트
- ✅ updatedAt 시간 변경 확인

---

### TC-3.5: 댓글 삭제 (DELETE /posts/{postId}/comments/{commentId})

**사전 조건**:
- 본인이 작성한 댓글 존재

**테스트 단계**:
1. 댓글 "삭제" 버튼 클릭
2. 확인 모달 "삭제" 클릭

**실제 결과**:
- ✅ DELETE /posts/7/comments/1 204 No Content
- ✅ 댓글 목록에서 즉시 제거
- ✅ commentCount -1

---

## Phase 4: 게시글 작성 및 수정

### TC-4.1: 이미지 업로드 (POST /images)

**사전 조건**:
- 로그인 완료
- 이미지 파일 준비 (JPG/PNG/GIF, 5MB 이하)

**테스트 단계**:
1. /board/write.html 접속
2. "이미지 선택" 버튼 클릭
3. 이미지 파일 선택
4. 미리보기 확인

**예상 결과**:
- POST /images 201 Created
- 응답: `{ imageId, imageUrl }`
- 미리보기 이미지 표시
- TTL 1시간 설정 (expires_at)

**1차 테스트 결과** (2025-10-17 17:00):
- ❌ API 응답 필드명 불일치
  - 백엔드: `{ imageId, imageUrl }`
  - 프론트엔드: `result.image_id`, `result.image_url`

**수정 내역** (커밋 de7aea8):
```javascript
// write.js (수정)
async function handleImageUpload(event) {
  const result = await uploadImage(file);
  uploadedImageId = result.imageId;  // ✅ camelCase
  showImagePreview(result.imageUrl);  // ✅ camelCase
}
```

**2차 테스트 결과**:
- ✅ POST /images 201 Created
- ✅ imageId 저장: 123
- ✅ 미리보기 표시 성공

**API 응답 예시**:
```json
{
  "message": "upload_image_success",
  "data": {
    "imageId": 123,
    "imageUrl": "https://ktb-3-community-images-dev.s3.ap-northeast-2.amazonaws.com/images/20251017/abc123.jpg"
  },
  "timestamp": "2025-10-17T17:05:00"
}
```

**DB 확인**:
```sql
SELECT * FROM images WHERE image_id = 123;
-- image_id: 123
-- image_url: https://...
-- expires_at: 2025-10-17 18:05:00  (1시간 후)
```

---

### TC-4.2: 게시글 작성 (POST /posts)

**사전 조건**:
- 로그인 완료
- (선택) 이미지 업로드 완료 (imageId=123)

**테스트 단계**:
1. /board/write.html 접속
2. 제목 입력: "Phase 4 테스트 게시글"
3. 내용 입력: "이미지 포함 게시글 테스트"
4. 이미지 선택 (TC-4.1 완료)
5. "작성완료" 버튼 클릭

**예상 결과**:
- POST /posts 201 Created
- 응답: `{ postId }`
- /board/detail.html?id={postId}로 리다이렉트
- 이미지 TTL 해제 (expires_at → NULL)

**1차 테스트 결과**:
- ❌ 필드명 불일치: `image_id` → `imageId`

**수정 내역** (커밋 db3b42f):
```javascript
// write.js (수정)
const post = await fetchWithAuth('/posts', {
  method: 'POST',
  body: JSON.stringify({
    title,
    content,
    imageId: uploadedImageId  // ✅ camelCase
  })
});
```

**2차 테스트 결과**:
- ✅ POST /posts 201 Created
- ✅ 리다이렉트 성공
- ✅ 이미지 표시 정상

**DB 확인**:
```sql
SELECT * FROM posts WHERE post_id = 8;
-- post_id: 8
-- post_title: "Phase 4 테스트 게시글"
-- post_content: "이미지 포함 게시글 테스트"

SELECT * FROM post_images WHERE post_id = 8;
-- post_id: 8
-- image_id: 123
-- display_order: 1

SELECT * FROM images WHERE image_id = 123;
-- expires_at: NULL  (✅ TTL 해제됨)
```

**증빙 자료**:
- `phase3_post_created.png` - 게시글 작성 성공

---

### TC-4.3: 게시글 수정 (PATCH /posts/{id})

**사전 조건**:
- 본인이 작성한 게시글 존재 (post_id=8)

**테스트 단계**:
1. 게시글 상세 페이지에서 "수정" 버튼 클릭
2. /board/edit.html?id=8 이동
3. 제목 수정: "수정된 게시글 제목"
4. 내용 수정: "수정된 내용입니다."
5. 이미지 변경 (선택)
6. "수정완료" 버튼 클릭

**실제 결과**:
- ✅ PATCH /posts/8 200 OK
- ✅ 게시글 상세 페이지로 리다이렉트
- ✅ 수정된 내용 확인
- ✅ updatedAt 시간 변경

---

### TC-4.4: 게시글 삭제 (DELETE /posts/{id})

**사전 조건**:
- 본인이 작성한 게시글 존재

**테스트 단계**:
1. 게시글 상세 페이지에서 "삭제" 버튼 클릭
2. 확인 모달 "삭제" 클릭

**실제 결과**:
- ✅ DELETE /posts/8 204 No Content
- ✅ 게시글 목록으로 리다이렉트
- ✅ DB: post_status = DELETED (Soft Delete)

---

## Phase 5: 프로필 관리

### TC-5.1: 프로필 조회 (GET /users/{id})

**사전 조건**:
- 로그인 완료

**테스트 단계**:
1. /user/profile-edit.html 접속
2. 현재 프로필 정보 로드 확인

**실제 결과**:
- ✅ GET /users/11 200 OK
- ✅ 닉네임, 이메일, 프로필 이미지 표시

---

### TC-5.2: 프로필 수정 (PATCH /users/{id})

**사전 조건**:
- 로그인 완료

**테스트 단계**:
1. /user/profile-edit.html 접속
2. 닉네임 수정: "수정된닉네임"
3. 프로필 이미지 변경 (선택)
4. "저장" 버튼 클릭

**1차 테스트 결과**:
- ❌ 필드명 불일치: `profile_image` → `profileImage`

**수정 내역** (커밋 db3b42f):
```javascript
// profile-edit.js (수정)
const formData = new FormData();
formData.append('nickname', nickname);
if (profileImage) {
  formData.append('profileImage', profileImage);  // ✅ camelCase
}
```

**2차 테스트 결과**:
- ✅ PATCH /users/11 200 OK
- ✅ 닉네임 변경 확인
- ✅ 프로필 이미지 변경 확인

**증빙 자료**:
- `phase4_profile_updated.png` - 프로필 수정 성공

---

### TC-5.3: 비밀번호 변경 (PATCH /users/{id}/password)

**사전 조건**:
- 로그인 완료

**테스트 단계**:
1. /user/password-change.html 접속
2. 새 비밀번호 입력: `NewPass1234!`
3. 새 비밀번호 확인 입력: `NewPass1234!`
4. "변경" 버튼 클릭

**1차 테스트 결과**:
- ❌ 필드명 불일치: `new_password` → `newPassword`

**수정 내역** (커밋 d7c7b61):
```javascript
// password-change.js (수정)
await fetchWithAuth(`/users/${userId}/password`, {
  method: 'PATCH',
  body: JSON.stringify({
    newPassword: newPassword,              // ✅ camelCase
    newPasswordConfirm: newPasswordConfirm // ✅ camelCase
  })
});
```

**2차 테스트 결과**:
- ✅ PATCH /users/11/password 200 OK
- ✅ 성공 메시지 표시
- ✅ 새 비밀번호로 재로그인 가능

**증빙 자료**:
- `phase4_password_changed.png` - 비밀번호 변경 성공

---

### TC-5.4: 회원 탈퇴 (PUT /users/{id})

**사전 조건**:
- 로그인 완료

**테스트 단계**:
1. /user/profile-edit.html 접속
2. "회원 탈퇴" 버튼 클릭
3. 1차 확인 모달 "탈퇴" 클릭
4. 2차 확인 모달 "최종 탈퇴" 클릭

**실제 결과**:
- ✅ PUT /users/11 200 OK
- ✅ localStorage 토큰 삭제
- ✅ 로그인 페이지로 리다이렉트
- ✅ DB: user_status = INACTIVE

---

## 이미지 업로드 통합 테스트

### Scenario 1: 게시글 이미지 업로드

**테스트 단계**:
1. POST /images → `{ imageId, imageUrl }`
2. POST /posts `{ imageId }` → 게시글 작성
3. GET /posts/{id} → 이미지 포함 게시글 조회

**결과**: ✅ 전체 성공

**참조**: `docs/test/IMAGE_UPLOAD_TEST_RESULTS.md` Scenario 1

---

### Scenario 2: 프로필 이미지 수정

**테스트 단계**:
1. PATCH /users/{id} (multipart)
   - FormData: `{ nickname, profileImage }`
2. GET /users/{id} → 프로필 이미지 확인

**결과**: ✅ 전체 성공

**참조**: `docs/test/IMAGE_UPLOAD_TEST_RESULTS.md` Scenario 2

---

### Scenario 3: 회원가입 시 프로필 이미지

**테스트 단계**:
1. POST /users/signup (multipart)
   - FormData: `{ email, password, nickname, profileImage }`
2. 자동 로그인 확인
3. 프로필 이미지 표시 확인

**결과**: ✅ 전체 성공

**참조**: `docs/test/IMAGE_UPLOAD_TEST_RESULTS.md` Scenario 3

---

## 버그 리포트

### Bug-1: JWT 토큰 localStorage 저장 실패

**발견 일시**: 2025-10-17 13:02
**심각도**: Critical
**영향 범위**: 회원가입, 로그인 전체
**수정 커밋**: a49ebcc

**재현 단계**:
1. 회원가입 또는 로그인 성공
2. localStorage 확인: `access_token`, `refresh_token` = "undefined"

**원인**:
- 백엔드 API 응답: camelCase (`accessToken`, `refreshToken`)
- 프론트엔드 코드: snake_case (`access_token`, `refresh_token`)

**수정**:
- `register.js` line 149-150
- `login.js` line 80-81
- snake_case → camelCase 통일

---

### Bug-2: "Entity not managed" (백엔드)

**발견 일시**: 2025-10-17 13:09
**심각도**: Critical
**영향 범위**: Phase 3-5 전체 블로킹
**수정 커밋**: 8103318, d2ae4f0

**재현 단계**:
1. 게시글 작성 성공 (POST /posts)
2. 게시글 상세 조회 (GET /posts/{id}) → 400 에러

**원인**:
- JPA detached entity 문제
- PostStats viewCount 증가 로직에서 Entity 분리

**수정**:
- Optimistic Update 패턴 도입
- JPQL UPDATE로 영속성 컨텍스트 우회
- 클라이언트 UI에서 viewCount +1 처리

---

### Bug-3: 이미지 업로드 필드명 불일치

**발견 일시**: 2025-10-17 17:00
**심각도**: High
**영향 범위**: 게시글 이미지, 프로필 이미지
**수정 커밋**: de7aea8, db3b42f

**재현 단계**:
1. POST /images 성공 → `{ imageId, imageUrl }`
2. POST /posts `{ image_id }` → 이미지 연결 실패

**원인**:
- API 응답: camelCase
- 프론트엔드 코드: snake_case

**수정**:
- `write.js`, `edit.js`, `profile-edit.js`, `register.js` 수정
- 4개 파일 필드명 통일

---

### Bug-4: 비밀번호 변경 필드명 불일치

**발견 일시**: 2025-10-17 18:00
**심각도**: Medium
**영향 범위**: 비밀번호 변경 기능
**수정 커밋**: d7c7b61

**원인**: API 요청 필드명 불일치

**수정**: `password-change.js` 필드명 camelCase 통일

---

### Bug-5: 댓글 카운트 off-by-one

**발견 일시**: 2025-10-17
**심각도**: Low
**영향 범위**: 댓글 카운트 표시
**수정 커밋**: a933570

**재현 단계**:
1. 댓글 작성 → commentCount = 2 (실제 1개)
2. 댓글 삭제 → commentCount = 0 (실제 1개)

**원인**: `state.comments.length ± 1` 중복 계산

**수정**: `prependComment()`, `removeCommentFromList()` 로직 수정

---

### Bug-6: 빈 src 속성

**발견 일시**: 2025-10-17
**심각도**: Low
**영향 범위**: 프로필 이미지 미설정 시
**수정 커밋**: ed30b88

**원인**: `<img src="">` 빈 문자열

**수정**: 기본 placeholder 이미지 추가

---

## 통계 및 메트릭

### API 테스트 커버리지

| 카테고리 | 전체 | 테스트 | 통과 | 통과율 |
|---------|------|--------|------|--------|
| 인증 | 3 | 2 | 2 | 66.7% |
| 사용자 | 5 | 4 | 4 | 80.0% |
| 게시글 | 5 | 5 | 5 | **100%** |
| 댓글 | 4 | 4 | 4 | **100%** |
| 좋아요 | 3 | 2 | 2 | 66.7% |
| 이미지 | 1 | 1 | 1 | **100%** |
| **합계** | **21** | **18** | **18** | **85.7%** |

### 버그 발견율

| 심각도 | 발견 수 | 수정 수 | 미해결 |
|--------|---------|---------|--------|
| Critical | 2 | 2 | 0 |
| High | 1 | 1 | 0 |
| Medium | 1 | 1 | 0 |
| Low | 2 | 2 | 0 |
| **합계** | **6** | **6** | **0** |

### 코드 메트릭

| 항목 | 값 |
|------|-----|
| JavaScript 파일 | 11개 |
| 총 코드 라인 | 3,258줄 |
| HTML 파일 | 12개 |
| CSS 파일 | 20개 |
| 커밋 수 | 30개 (1주일) |
| 스크린샷 | 9개 |

---

## 결론

### 성과 요약

1. ✅ **프론트엔드 구현 100% 완료**
   - Vanilla JS + REST API 연동
   - 11개 파일, 3,258줄

2. ✅ **백엔드 API 연동 85.7% 완료**
   - 18/21개 API 테스트 통과
   - 핵심 기능 전체 동작 확인

3. ✅ **버그 수정 100% 완료**
   - 6건 발견, 6건 전체 수정
   - Critical 버그 0건

4. ✅ **아키텍처 개선**
   - Optimistic Update 패턴 도입
   - 성능 및 UX 향상

### 향후 과제

1. **미테스트 API (3개)**
   - POST /auth/logout
   - POST /auth/refresh_token (자동 처리 검증 필요)
   - GET /posts/users/me/likes

2. **추가 테스트 시나리오**
   - 에러 처리 테스트 (401, 403, 404, 409)
   - Rate Limiting 테스트
   - 동시성 테스트 (좋아요, 댓글)

3. **성능 테스트**
   - 페이지 로드 시간
   - API 응답 시간
   - 이미지 업로드 속도

4. **크로스 브라우저 테스트**
   - Chrome (완료)
   - Safari, Firefox, Edge

---

## 참조 문서

- `docs/test/INTEGRATION_TEST.md` - 연동 테스트 가이드
- `docs/test/IMAGE_UPLOAD_TEST_RESULTS.md` - 이미지 업로드 테스트
- `docs/test/result/phase1-report.md` - Phase 1 상세 리포트
- `docs/test/result/phase2-3-report.md` - Phase 2-3 상세 리포트
- `docs/fe/PLAN.md` - 프론트엔드 구현 현황
- `docs/be/API.md` - REST API 명세
- `docs/be/LLD.md` - 백엔드 아키텍처

---

**문서 버전**: 1.0
**최종 수정**: 2025-10-18
**작성자**: 개발팀
