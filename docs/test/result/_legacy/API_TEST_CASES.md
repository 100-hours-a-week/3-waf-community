# API 테스트 케이스 상세 명세서

## 문서 정보

| 항목 | 내용 |
|------|------|
| 문서 유형 | API 테스트 케이스 명세 |
| 테스트 대상 | REST API 21개 엔드포인트 |
| 테스트 완료 | 18개 (85.7%) |
| 작성일 | 2025-10-18 |
| 버전 | 1.0 |

---

## 테스트 케이스 목차

### 1. 인증 (Authentication) - 3개 API
- [TC-AUTH-001](#tc-auth-001) POST /auth/login
- [TC-AUTH-002](#tc-auth-002) POST /auth/logout
- [TC-AUTH-003](#tc-auth-003) POST /auth/refresh_token

### 2. 사용자 (Users) - 5개 API
- [TC-USER-001](#tc-user-001) POST /users/signup
- [TC-USER-002](#tc-user-002) GET /users/{id}
- [TC-USER-003](#tc-user-003) PATCH /users/{id}
- [TC-USER-004](#tc-user-004) PATCH /users/{id}/password
- [TC-USER-005](#tc-user-005) PUT /users/{id}

### 3. 게시글 (Posts) - 5개 API
- [TC-POST-001](#tc-post-001) GET /posts
- [TC-POST-002](#tc-post-002) GET /posts/{id}
- [TC-POST-003](#tc-post-003) POST /posts
- [TC-POST-004](#tc-post-004) PATCH /posts/{id}
- [TC-POST-005](#tc-post-005) DELETE /posts/{id}

### 4. 댓글 (Comments) - 4개 API
- [TC-COMMENT-001](#tc-comment-001) GET /posts/{postId}/comments
- [TC-COMMENT-002](#tc-comment-002) POST /posts/{postId}/comments
- [TC-COMMENT-003](#tc-comment-003) PATCH /posts/{postId}/comments/{commentId}
- [TC-COMMENT-004](#tc-comment-004) DELETE /posts/{postId}/comments/{commentId}

### 5. 좋아요 (Likes) - 3개 API
- [TC-LIKE-001](#tc-like-001) POST /posts/{id}/like
- [TC-LIKE-002](#tc-like-002) DELETE /posts/{id}/like
- [TC-LIKE-003](#tc-like-003) GET /posts/users/me/likes

### 6. 이미지 (Images) - 1개 API
- [TC-IMAGE-001](#tc-image-001) POST /images

---

## 1. 인증 (Authentication)

### TC-AUTH-001

**API**: POST /auth/login
**설명**: 이메일/비밀번호로 로그인하여 JWT 토큰 발급
**테스트 일자**: 2025-10-18
**상태**: ✅ 통과

#### 사전 조건
- 사용자 계정 존재 (user_id=11, email=test@startupcode.kr)
- 비밀번호 해시 일치

#### 요청

```http
POST /auth/login HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "email": "test@startupcode.kr",
  "password": "Test1234!"
}
```

#### 예상 응답

**성공 (200 OK)**:
```json
{
  "message": "login_success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "timestamp": "2025-10-18T17:15:00"
}
```

**실패 (401 Unauthorized)**:
```json
{
  "message": "AUTH-001",
  "data": {
    "details": "Invalid credentials"
  },
  "timestamp": "2025-10-18T17:15:00"
}
```

#### 검증 항목
- [x] HTTP 상태 코드 200
- [x] accessToken 존재 및 형식 검증 (JWT)
- [x] refreshToken 존재 및 형식 검증
- [x] localStorage 저장 확인
- [x] /board/list.html 리다이렉트

#### 실제 결과
- ✅ 200 OK
- ✅ accessToken 길이: 185자
- ✅ refreshToken 길이: 127자
- ✅ localStorage 저장 성공
- ✅ 리다이렉트 성공

#### 참조
- API.md Section 1.1
- `login.js` line 80-98

---

### TC-AUTH-002

**API**: POST /auth/logout
**설명**: Refresh Token을 무효화하고 로그아웃
**테스트 일자**: 미테스트
**상태**: ⏸️ 미테스트

#### 사전 조건
- 로그인 완료 (access_token, refresh_token 존재)

#### 요청

```http
POST /auth/logout HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJ...
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### 예상 응답

**성공 (200 OK)**:
```json
{
  "message": "logout_success",
  "data": null,
  "timestamp": "2025-10-18T17:20:00"
}
```

#### 검증 항목
- [ ] HTTP 상태 코드 200
- [ ] localStorage 토큰 삭제
- [ ] /user/login.html 리다이렉트
- [ ] DB: user_tokens 테이블에서 refresh_token 삭제

#### 미테스트 사유
- 테스트 우선순위 낮음 (핵심 기능 아님)
- 로그인/회원가입 정상 작동 확인 우선

---

### TC-AUTH-003

**API**: POST /auth/refresh_token
**설명**: Refresh Token으로 새 Access Token 발급
**테스트 일자**: 자동 처리 중
**상태**: 🔄 자동 테스트

#### 사전 조건
- refresh_token 존재 (만료되지 않음)

#### 요청

```http
POST /auth/refresh_token HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### 예상 응답

**성공 (200 OK)**:
```json
{
  "message": "token_refreshed",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "timestamp": "2025-10-18T17:25:00"
}
```

#### 검증 항목
- [x] fetchWithAuth가 401 응답 시 자동 호출
- [x] 새 access_token localStorage 저장
- [x] 원래 요청 자동 재시도

#### 실제 결과
- ✅ fetchWithAuth 내부에서 자동 처리 확인
- ✅ 401 → refresh → retry 플로우 정상

#### 참조
- `api.js` line 45-74 (refreshAccessToken 함수)

---

## 2. 사용자 (Users)

### TC-USER-001

**API**: POST /users/signup
**설명**: 회원가입 및 자동 로그인
**테스트 일자**: 2025-10-18
**상태**: ✅ 통과

#### 사전 조건
- 이메일 미사용 상태
- 닉네임 미사용 상태

#### 요청

```http
POST /users/signup HTTP/1.1
Host: localhost:8080
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="email"

test@startupcode.kr
------WebKitFormBoundary
Content-Disposition: form-data; name="password"

Test1234!
------WebKitFormBoundary
Content-Disposition: form-data; name="nickname"

테스터
------WebKitFormBoundary
Content-Disposition: form-data; name="profileImage"; filename="profile.jpg"
Content-Type: image/jpeg

[Binary Data]
------WebKitFormBoundary--
```

#### 예상 응답

**성공 (201 Created)**:
```json
{
  "message": "register_success",
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ..."
  },
  "timestamp": "2025-10-18T17:30:00"
}
```

**실패 (409 Conflict - 이메일 중복)**:
```json
{
  "message": "USER-002",
  "data": {
    "details": "Email already exists: test@startupcode.kr"
  },
  "timestamp": "2025-10-18T17:30:00"
}
```

**실패 (409 Conflict - 닉네임 중복)**:
```json
{
  "message": "USER-003",
  "data": {
    "details": "Nickname already exists: 테스터"
  },
  "timestamp": "2025-10-18T17:30:00"
}
```

#### 검증 항목
- [x] HTTP 상태 코드 201
- [x] accessToken, refreshToken 발급
- [x] localStorage 저장
- [x] DB users 테이블 insert 확인
- [x] 프로필 이미지 S3 업로드 확인
- [x] /board/list.html 리다이렉트

#### 실제 결과
- ✅ 201 Created
- ✅ user_id=11 생성
- ✅ 프로필 이미지 업로드 성공 (선택 시)
- ✅ 자동 로그인 성공

#### 버그 이력
- **Bug-1** (커밋 a49ebcc): 필드명 불일치 (snake_case → camelCase)
  - 수정 전: `response.access_token` → undefined
  - 수정 후: `response.accessToken` → 정상

#### 참조
- API.md Section 2.1
- `register.js` line 127-177

---

### TC-USER-002

**API**: GET /users/{id}
**설명**: 사용자 프로필 조회
**테스트 일자**: 2025-10-18
**상태**: ✅ 통과

#### 사전 조건
- 사용자 존재 (user_id=11)

#### 요청

```http
GET /users/11 HTTP/1.1
Host: localhost:8080
```

#### 예상 응답

**성공 (200 OK)**:
```json
{
  "message": "get_profile_success",
  "data": {
    "userId": 11,
    "email": "test@startupcode.kr",
    "nickname": "테스터",
    "profileImage": "https://..."
  },
  "timestamp": "2025-10-18T17:35:00"
}
```

**실패 (404 Not Found)**:
```json
{
  "message": "USER-001",
  "data": {
    "details": "User not found with id: 999"
  },
  "timestamp": "2025-10-18T17:35:00"
}
```

#### 검증 항목
- [x] HTTP 상태 코드 200
- [x] userId, email, nickname, profileImage 필드 존재
- [x] 프로필 이미지 URL 유효성

#### 실제 결과
- ✅ 200 OK
- ✅ 모든 필드 정상 반환
- ✅ profileImage null (미설정 시)

---

### TC-USER-003

**API**: PATCH /users/{id}
**설명**: 사용자 프로필 수정
**테스트 일자**: 2025-10-18
**상태**: ✅ 통과

#### 사전 조건
- 로그인 완료 (본인 계정)

#### 요청

```http
PATCH /users/11 HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJ...
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="nickname"

수정된닉네임
------WebKitFormBoundary
Content-Disposition: form-data; name="profileImage"; filename="new-profile.jpg"
Content-Type: image/jpeg

[Binary Data]
------WebKitFormBoundary--
```

#### 예상 응답

**성공 (200 OK)**:
```json
{
  "message": "update_profile_success",
  "data": {
    "userId": 11,
    "nickname": "수정된닉네임",
    "profileImage": "https://..."
  },
  "timestamp": "2025-10-18T17:40:00"
}
```

**실패 (409 Conflict - 닉네임 중복)**:
```json
{
  "message": "USER-003",
  "data": {
    "details": "Nickname already exists: 수정된닉네임"
  },
  "timestamp": "2025-10-18T17:40:00"
}
```

#### 검증 항목
- [x] HTTP 상태 코드 200
- [x] 닉네임 변경 확인
- [x] 프로필 이미지 변경 확인
- [x] DB users 테이블 update 확인

#### 실제 결과
- ✅ 200 OK
- ✅ 닉네임 변경 성공
- ✅ 프로필 이미지 S3 업로드 및 URL 업데이트

#### 버그 이력
- **Bug-3** (커밋 db3b42f): 필드명 불일치
  - 수정 전: `formData.append('profile_image', file)`
  - 수정 후: `formData.append('profileImage', file)`

#### 참조
- API.md Section 2.3
- `profile-edit.js` line 85-135
- 증빙: `phase4_profile_updated.png`

---

### TC-USER-004

**API**: PATCH /users/{id}/password
**설명**: 비밀번호 변경
**테스트 일자**: 2025-10-18
**상태**: ✅ 통과

#### 사전 조건
- 로그인 완료 (본인 계정)

#### 요청

```http
PATCH /users/11/password HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJ...
Content-Type: application/json

{
  "newPassword": "NewPass1234!",
  "newPasswordConfirm": "NewPass1234!"
}
```

#### 예상 응답

**성공 (200 OK)**:
```json
{
  "message": "update_password_success",
  "data": null,
  "timestamp": "2025-10-18T17:45:00"
}
```

**실패 (400 Bad Request - 비밀번호 정책 위반)**:
```json
{
  "message": "USER-004",
  "data": {
    "details": "Password must be 8-20 characters with uppercase, lowercase, and special character"
  },
  "timestamp": "2025-10-18T17:45:00"
}
```

#### 검증 항목
- [x] HTTP 상태 코드 200
- [x] 비밀번호 정책 검증 (8-20자, 대/소/특수)
- [x] DB password_hash 업데이트 확인
- [x] 새 비밀번호로 재로그인 가능

#### 실제 결과
- ✅ 200 OK
- ✅ 비밀번호 변경 성공
- ✅ 새 비밀번호로 로그인 확인

#### 버그 이력
- **Bug-4** (커밋 d7c7b61): 필드명 불일치
  - 수정 전: `new_password`, `new_password_confirm`
  - 수정 후: `newPassword`, `newPasswordConfirm`

#### 참조
- API.md Section 2.4
- `password-change.js` line 95-135
- 증빙: `phase4_password_changed.png`

---

### TC-USER-005

**API**: PUT /users/{id}
**설명**: 회원 탈퇴 (계정 비활성화)
**테스트 일자**: 2025-10-18
**상태**: ✅ 통과

#### 사전 조건
- 로그인 완료 (본인 계정)

#### 요청

```http
PUT /users/11 HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJ...
```

#### 예상 응답

**성공 (200 OK)**:
```json
{
  "message": "account_deactivated_success",
  "data": null,
  "timestamp": "2025-10-18T17:50:00"
}
```

#### 검증 항목
- [x] HTTP 상태 코드 200
- [x] DB user_status = INACTIVE
- [x] localStorage 토큰 삭제
- [x] /user/login.html 리다이렉트

#### 실제 결과
- ✅ 200 OK
- ✅ user_status 변경 확인
- ✅ 로그인 페이지 리다이렉트

#### 참조
- API.md Section 2.5
- `profile-edit.js` line 137-167

---

## 3. 게시글 (Posts)

### TC-POST-001

**API**: GET /posts
**설명**: 게시글 목록 조회 (Cursor 페이지네이션)
**테스트 일자**: 2025-10-18
**상태**: ✅ 통과

#### 사전 조건
- DB에 게시글 0개 이상

#### 요청

```http
GET /posts?cursor=null&limit=10&sort=latest HTTP/1.1
Host: localhost:8080
```

#### 예상 응답

**성공 (200 OK)**:
```json
{
  "message": "get_posts_success",
  "data": {
    "posts": [
      {
        "postId": 7,
        "title": "게시글 제목",
        "content": "게시글 내용...",
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
  "timestamp": "2025-10-18T17:55:00"
}
```

#### 검증 항목
- [x] HTTP 상태 코드 200
- [x] posts 배열 존재
- [x] nextCursor, hasMore 필드 존재
- [x] 게시글 렌더링 (제목, 작성자, 날짜, 통계)
- [x] "더보기" 버튼 동작

#### 실제 결과
- ✅ 200 OK
- ✅ Cursor 페이지네이션 정상
- ✅ nextCursor=null, hasMore=false (데이터 부족)

#### 참조
- API.md Section 3.1
- `list.js` line 75-120

---

### TC-POST-002

**API**: GET /posts/{id}
**설명**: 게시글 상세 조회
**테스트 일자**: 2025-10-18
**상태**: ✅ 통과

#### 사전 조건
- 게시글 존재 (post_id=7)

#### 요청

```http
GET /posts/7 HTTP/1.1
Host: localhost:8080
```

#### 예상 응답

**성공 (200 OK)**:
```json
{
  "message": "get_post_detail_success",
  "data": {
    "postId": 7,
    "title": "게시글 제목",
    "content": "게시글 내용...",
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
  "timestamp": "2025-10-18T18:00:00"
}
```

**실패 (404 Not Found)**:
```json
{
  "message": "POST-001",
  "data": {
    "details": "Post not found with id: 999"
  },
  "timestamp": "2025-10-18T18:00:00"
}
```

#### 검증 항목
- [x] HTTP 상태 코드 200
- [x] 모든 필드 렌더링
- [x] Optimistic Update (viewCount +1 UI 표시)
- [x] F5 새로고침 시 정확한 값 동기화

#### 실제 결과
- ✅ 200 OK
- ✅ viewCount Optimistic Update 동작 확인
  - 서버 응답: 0
  - UI 표시: 1
  - F5 후: 1 (동기화 완료)

#### 버그 이력
- **Bug-2** (커밋 8103318): "Entity not managed" 백엔드 버그
  - 1차 테스트: 400 Bad Request
  - 수정: Optimistic Update 패턴 도입
  - 2차 테스트: 200 OK

#### 참조
- API.md Section 3.2
- `detail.js` line 75-140
- 증빙: `phase2_post_detail_page.png`

---

### TC-POST-003

**API**: POST /posts
**설명**: 게시글 작성
**테스트 일자**: 2025-10-18
**상태**: ✅ 통과

#### 사전 조건
- 로그인 완료
- (선택) 이미지 업로드 완료 (imageId=123)

#### 요청

```http
POST /posts HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJ...
Content-Type: application/json

{
  "title": "게시글 제목",
  "content": "게시글 내용입니다.",
  "imageId": 123
}
```

#### 예상 응답

**성공 (201 Created)**:
```json
{
  "message": "create_post_success",
  "data": {
    "postId": 8
  },
  "timestamp": "2025-10-18T18:05:00"
}
```

#### 검증 항목
- [x] HTTP 상태 코드 201
- [x] postId 반환
- [x] DB posts 테이블 insert
- [x] DB post_stats 초기화 (0/0/0)
- [x] 이미지 연결 (post_images 테이블)
- [x] 이미지 TTL 해제 (expires_at → NULL)
- [x] /board/detail.html?id=8 리다이렉트

#### 실제 결과
- ✅ 201 Created
- ✅ post_id=8 생성
- ✅ post_stats 초기화 확인
- ✅ 이미지 TTL 해제 확인

#### 버그 이력
- **Bug-3** (커밋 db3b42f): 필드명 불일치
  - 수정 전: `image_id`
  - 수정 후: `imageId`

#### 참조
- API.md Section 3.3
- `write.js` line 175-210
- 증빙: `phase3_post_created.png`

---

### TC-POST-004

**API**: PATCH /posts/{id}
**설명**: 게시글 수정
**테스트 일자**: 2025-10-18
**상태**: ✅ 통과

#### 사전 조건
- 로그인 완료 (본인 게시글)

#### 요청

```http
PATCH /posts/8 HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJ...
Content-Type: application/json

{
  "title": "수정된 제목",
  "content": "수정된 내용",
  "imageId": 124
}
```

#### 예상 응답

**성공 (200 OK)**:
```json
{
  "message": "update_post_success",
  "data": {
    "postId": 8,
    "title": "수정된 제목",
    "content": "수정된 내용",
    "updatedAt": "2025-10-18T18:10:00"
  },
  "timestamp": "2025-10-18T18:10:00"
}
```

**실패 (403 Forbidden - 권한 없음)**:
```json
{
  "message": "POST-002",
  "data": {
    "details": "Not authorized to update this post"
  },
  "timestamp": "2025-10-18T18:10:00"
}
```

#### 검증 항목
- [x] HTTP 상태 코드 200
- [x] 제목/내용 변경 확인
- [x] 이미지 변경 확인
- [x] updatedAt 시간 업데이트

#### 실제 결과
- ✅ 200 OK
- ✅ 모든 필드 정상 수정

---

### TC-POST-005

**API**: DELETE /posts/{id}
**설명**: 게시글 삭제 (Soft Delete)
**테스트 일자**: 2025-10-18
**상태**: ✅ 통과

#### 사전 조건
- 로그인 완료 (본인 게시글)

#### 요청

```http
DELETE /posts/8 HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJ...
```

#### 예상 응답

**성공 (204 No Content)**:
- HTTP 204, 응답 body 없음

**실패 (403 Forbidden)**:
```json
{
  "message": "POST-002",
  "data": {
    "details": "Not authorized to delete this post"
  },
  "timestamp": "2025-10-18T18:15:00"
}
```

#### 검증 항목
- [x] HTTP 상태 코드 204
- [x] DB post_status = DELETED
- [x] /board/list.html 리다이렉트
- [x] 목록에서 제거 확인

#### 실제 결과
- ✅ 204 No Content
- ✅ Soft Delete 확인
- ✅ 리다이렉트 성공

---

## 4. 댓글 (Comments)

### TC-COMMENT-001

**API**: GET /posts/{postId}/comments
**설명**: 댓글 목록 조회
**테스트 일자**: 2025-10-18
**상태**: ✅ 통과

#### 요청

```http
GET /posts/7/comments?offset=0&limit=10 HTTP/1.1
Host: localhost:8080
```

#### 예상 응답

**성공 (200 OK)**:
```json
{
  "message": "get_comments_success",
  "data": {
    "comments": [
      {
        "commentId": 1,
        "content": "첫 번째 댓글",
        "author": {
          "userId": 11,
          "nickname": "테스터",
          "profileImage": null
        },
        "createdAt": "2025-10-18T18:20:00",
        "updatedAt": "2025-10-18T18:20:00"
      }
    ],
    "pagination": {
      "totalCount": 1
    }
  },
  "timestamp": "2025-10-18T18:20:00"
}
```

#### 검증 항목
- [x] HTTP 상태 코드 200
- [x] comments 배열 존재
- [x] pagination.totalCount 존재
- [x] 댓글 렌더링

#### 실제 결과
- ✅ 200 OK
- ✅ Offset 페이지네이션 정상

---

### TC-COMMENT-002

**API**: POST /posts/{postId}/comments
**설명**: 댓글 작성
**테스트 일자**: 2025-10-18
**상태**: ✅ 통과

#### 요청

```http
POST /posts/7/comments HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJ...
Content-Type: application/json

{
  "comment": "첫 번째 댓글입니다."
}
```

#### 예상 응답

**성공 (201 Created)**:
```json
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
    "createdAt": "2025-10-18T18:25:00",
    "updatedAt": "2025-10-18T18:25:00"
  },
  "timestamp": "2025-10-18T18:25:00"
}
```

#### 검증 항목
- [x] HTTP 상태 코드 201
- [x] commentId 반환
- [x] 댓글 목록 최상단 추가
- [x] commentCount +1

#### 실제 결과
- ✅ 201 Created
- ✅ 실시간 댓글 추가 성공

#### 버그 이력
- **Bug-5** (커밋 a933570): 댓글 카운트 off-by-one
  - 수정 전: `state.comments.length + 1`
  - 수정 후: `state.comments.length`

#### 참조
- API.md Section 5.2
- `detail.js` line 380-425
- 증빙: `phase2_comment_created.png`

---

### TC-COMMENT-003

**API**: PATCH /posts/{postId}/comments/{commentId}
**설명**: 댓글 수정
**테스트 일자**: 2025-10-18
**상태**: ✅ 통과

#### 요청

```http
PATCH /posts/7/comments/1 HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJ...
Content-Type: application/json

{
  "comment": "수정된 댓글입니다."
}
```

#### 예상 응답

**성공 (200 OK)**:
```json
{
  "message": "update_comment_success",
  "data": {
    "commentId": 1,
    "content": "수정된 댓글입니다.",
    "updatedAt": "2025-10-18T18:30:00"
  },
  "timestamp": "2025-10-18T18:30:00"
}
```

#### 검증 항목
- [x] HTTP 상태 코드 200
- [x] 댓글 내용 즉시 업데이트
- [x] updatedAt 시간 변경

#### 실제 결과
- ✅ 200 OK
- ✅ 댓글 수정 성공

---

### TC-COMMENT-004

**API**: DELETE /posts/{postId}/comments/{commentId}
**설명**: 댓글 삭제
**테스트 일자**: 2025-10-18
**상태**: ✅ 통과

#### 요청

```http
DELETE /posts/7/comments/1 HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJ...
```

#### 예상 응답

**성공 (204 No Content)**:
- HTTP 204, 응답 body 없음

#### 검증 항목
- [x] HTTP 상태 코드 204
- [x] 댓글 목록에서 제거
- [x] commentCount -1

#### 실제 결과
- ✅ 204 No Content
- ✅ 댓글 삭제 성공

---

## 5. 좋아요 (Likes)

### TC-LIKE-001

**API**: POST /posts/{id}/like
**설명**: 좋아요 추가
**테스트 일자**: 2025-10-18
**상태**: ✅ 통과

#### 요청

```http
POST /posts/7/like HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJ...
```

#### 예상 응답

**성공 (200 OK)** (Phase 5 이후):
```json
{
  "message": "like_success",
  "data": {
    "message": "like_success"
  },
  "timestamp": "2025-10-18T18:35:00"
}
```

**실패 (409 Conflict - 이미 좋아요함)**:
```json
{
  "message": "LIKE-001",
  "data": {
    "details": "Already liked this post"
  },
  "timestamp": "2025-10-18T18:35:00"
}
```

#### 검증 항목
- [x] HTTP 상태 코드 200
- [x] UI 즉시 업데이트 (Optimistic Update)
- [x] likeCount +1
- [x] 버튼 스타일 변경

#### 실제 결과
- ✅ 200 OK
- ✅ Optimistic Update 동작 확인

#### 참조
- API.md Section 6.1
- `detail.js` line 260-295

---

### TC-LIKE-002

**API**: DELETE /posts/{id}/like
**설명**: 좋아요 취소
**테스트 일자**: 2025-10-18
**상태**: ✅ 통과

#### 요청

```http
DELETE /posts/7/like HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJ...
```

#### 예상 응답

**성공 (200 OK)**:
```json
{
  "message": "unlike_success",
  "data": {
    "message": "unlike_success"
  },
  "timestamp": "2025-10-18T18:40:00"
}
```

#### 검증 항목
- [x] HTTP 상태 코드 200
- [x] UI 즉시 업데이트
- [x] likeCount -1

#### 실제 결과
- ✅ 200 OK
- ✅ 좋아요 취소 성공

---

### TC-LIKE-003

**API**: GET /posts/users/me/likes
**설명**: 내가 좋아요한 게시글 목록
**테스트 일자**: 미테스트
**상태**: ⏸️ 미테스트

#### 요청

```http
GET /posts/users/me/likes?offset=0&limit=10 HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJ...
```

#### 예상 응답

**성공 (200 OK)**:
```json
{
  "message": "get_liked_posts_success",
  "data": {
    "posts": [
      {
        "postId": 7,
        "title": "게시글 제목",
        "likedAt": "2025-10-18T18:35:00"
      }
    ],
    "pagination": {
      "totalCount": 1
    }
  },
  "timestamp": "2025-10-18T18:45:00"
}
```

#### 미테스트 사유
- UI 미구현 (좋아요 목록 페이지 없음)
- 테스트 우선순위 낮음

---

## 6. 이미지 (Images)

### TC-IMAGE-001

**API**: POST /images
**설명**: 이미지 업로드 (S3 + TTL 1시간)
**테스트 일자**: 2025-10-17
**상태**: ✅ 통과

#### 사전 조건
- 로그인 완료
- 이미지 파일 준비 (JPG/PNG/GIF, 5MB 이하)

#### 요청

```http
POST /images HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJ...
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="test.jpg"
Content-Type: image/jpeg

[Binary Data]
------WebKitFormBoundary--
```

#### 예상 응답

**성공 (201 Created)**:
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

**실패 (413 Payload Too Large)**:
```json
{
  "message": "IMAGE-002",
  "data": {
    "details": "File size exceeds 5MB"
  },
  "timestamp": "2025-10-17T17:05:00"
}
```

**실패 (400 Bad Request - 파일 형식)**:
```json
{
  "message": "IMAGE-003",
  "data": {
    "details": "Invalid file type. Allowed: JPG, PNG, GIF"
  },
  "timestamp": "2025-10-17T17:05:00"
}
```

#### 검증 항목
- [x] HTTP 상태 코드 201
- [x] imageId, imageUrl 반환
- [x] S3 업로드 확인
- [x] DB images 테이블 insert
- [x] expires_at = NOW() + 1시간

#### 실제 결과
- ✅ 201 Created
- ✅ S3 업로드 성공
- ✅ TTL 설정 확인

#### 버그 이력
- **Bug-3** (커밋 de7aea8): 응답 필드명 불일치
  - 수정 전: `image_id`, `image_url`
  - 수정 후: `imageId`, `imageUrl`

#### 참조
- API.md Section 4.1
- `write.js` line 135-170
- IMAGE_UPLOAD_TEST_RESULTS.md

---

## 테스트 커버리지 요약

### 카테고리별

| 카테고리 | 전체 | 테스트 | 통과 | 미테스트 | 통과율 |
|---------|------|--------|------|----------|--------|
| 인증 | 3 | 2 | 2 | 1 | 66.7% |
| 사용자 | 5 | 4 | 4 | 1 | 80.0% |
| 게시글 | 5 | 5 | 5 | 0 | 100% |
| 댓글 | 4 | 4 | 4 | 0 | 100% |
| 좋아요 | 3 | 2 | 2 | 1 | 66.7% |
| 이미지 | 1 | 1 | 1 | 0 | 100% |
| **합계** | **21** | **18** | **18** | **3** | **85.7%** |

### 미테스트 API

1. **TC-AUTH-002**: POST /auth/logout
   - 사유: 우선순위 낮음

2. **TC-LIKE-003**: GET /posts/users/me/likes
   - 사유: UI 미구현

3. **TC-AUTH-003**: POST /auth/refresh_token
   - 상태: 자동 처리 중 (fetchWithAuth 내부)

---

## 참조 문서

- `docs/test/result/FULL_TEST_REPORT.md` - 전체 테스트 리포트
- `docs/test/INTEGRATION_TEST.md` - 연동 테스트 가이드
- `docs/test/IMAGE_UPLOAD_TEST_RESULTS.md` - 이미지 업로드 테스트
- `docs/be/API.md` - REST API 명세
- `docs/be/LLD.md` - 백엔드 아키텍처

---

**문서 버전**: 1.0
**최종 수정**: 2025-10-18
**작성자**: 개발팀
