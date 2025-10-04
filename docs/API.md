# 카부캠 커뮤니티 REST API 문서

## 목차
- [1. 인증 (Authentication)](#1-인증-authentication)
- [2. 사용자 (Users)](#2-사용자-users)
- [3. 게시글 (Posts)](#3-게시글-posts)
- [4. 이미지 (Images)](#4-이미지-images)
- [5. 댓글 (Comments)](#5-댓글-comments)
- [6. 좋아요 (Likes)](#6-좋아요-likes)
- [7. 공통 사양](#7-공통-사양)

---

## 1. 인증 (Authentication)

### 1.1 로그인
**Endpoint:** `POST /auth/login`

**Request:** `{ "email": "test@startupcode.kr", "password": "test1234" }`

**필수:** email(String), password(String)

**응답:**
- 200: `login_success` → access_token, refresh_token 반환
- 401: `invalid_credentials` (잘못된 인증 정보)
- 기타: [공통 응답 코드](#응답-코드) 참조

---

### 1.2 로그아웃
**Endpoint:** `POST /auth/logout`

**Request:** `{ "refresh_token": "..." }`

**필수:** refresh_token(String)

**응답:**
- 200: `logout_success`
- 401: `invalid_refresh_token` (유효하지 않은 리프레시 토큰)
- 기타: [공통 응답 코드](#응답-코드) 참조

---

### 1.3 액세스 토큰 재발급
**Endpoint:** `POST /auth/refresh_token`

**Request:** `{ "refresh_token": "..." }`

**필수:** refresh_token(String)

**응답:**
- 200: `token_refreshed` → access_token 반환
- 401: `invalid_refresh_token`
- 기타: [공통 응답 코드](#응답-코드) 참조

---

## 2. 사용자 (Users)

### 2.1 회원가입
**Endpoint:** `POST /users/signup` or `POST /users`

**Request:** `{ "email": "...", "password": "...", "nickname": "...", "profile_image": "..." }`

**필수:** email(String), password(String), nickname(String)  
**선택:** profile_image(String)

**응답:**
- 201: `register_success` → access_token, refresh_token 반환 (자동 로그인)
- 409: `resource_already_exists` → data.field: ["email"] or ["nickname"]
- 기타: [공통 응답 코드](#응답-코드) 참조

---

### 2.2 사용자 정보 조회
**Endpoint:** `GET /users/{userID}`

**헤더:** Authorization: Bearer {access_token}

**응답:**
- 200: `get_profile_success` → image, nickname, email 반환
- 기타: [공통 응답 코드](#응답-코드) 참조

---

### 2.3 사용자 정보 수정
**Endpoint:** `PATCH /users/{userID}`

**헤더:** Authorization: Bearer {access_token}

**Request:** `{ "nickname": "...", "profile_image": "..." }`

**선택:** nickname(String), profile_image(String)

**응답:**
- 200: `update_profile_success` → 수정된 정보 반환
- 409: `resource_already_exists` (닉네임 중복)
- 기타: [공통 응답 코드](#응답-코드) 참조

---

### 2.4 비밀번호 변경
**Endpoint:** `PATCH /users/{userID}/password`

**헤더:** Authorization: Bearer {access_token}

**Request:** `{ "new_password": "...", "new_password_confirm": "..." }`

**필수:** new_password(String), new_password_confirm(String)

**응답:**
- 200: `update_password_success`
- 400: 비밀번호 정책 위반 또는 불일치
- 기타: [공통 응답 코드](#응답-코드) 참조

---

### 2.5 회원 탈퇴 (비활성화)
**Endpoint:** `PUT /users/{userID}`

**헤더:** Authorization: Bearer {access_token}

**Request:** `{ "user_status": "INACTIVE" }`

**필수:** user_status(String) - "INACTIVE" (DDL: ACTIVE, INACTIVE, DELETED)

**응답:**
- 200: `account_deactivated_success`
- 기타: [공통 응답 코드](#응답-코드) 참조

---

## 3. 게시글 (Posts)

### 3.1 게시글 목록 조회
**Endpoint:** `GET /posts?offset=0&limit=10&sort=latest`

**쿼리:** offset(Number), limit(Number), sort(String: latest|likes)

**응답:**
- 200: `get_posts_success` → posts[], pagination.total_count
- 기타: [공통 응답 코드](#응답-코드) 참조

**데이터 구조:**
```json
{
  "posts": [{
    "postId": 123,
    "title": "...",
    "content": "...",
    "createdAt": "2025-09-30T10:00:00Z",
    "updatedAt": "2025-09-30T10:00:00Z",
    "author": { "userId": 1, "nickname": "...", "profileImage": "..." },
    "stats": { "likeCount": 42, "commentCount": 15, "viewCount": 230 }
  }],
  "pagination": { "total_count": 150 }
}
```

---

### 3.2 특정 게시글 상세 조회
**Endpoint:** `GET /posts/{postId}`

**응답:**
- 200: `get_post_detail_success` → 게시글 상세 정보 (작성자 정보 포함)
- 404: `post_not_found`
- 기타: [공통 응답 코드](#응답-코드) 참조

---

### 3.3 새 게시글 작성
**Endpoint:** `POST /posts`

**헤더:** Authorization: Bearer {access_token}

**Request:** `{ "title": "...", "content": "...", "image_id": 1 }`

**필수:** title(String), content(String)  
**선택:** image_id(Number) - POST /images로 먼저 업로드 필요

**응답:**
- 201: `create_post_success` → postId 반환
- 기타: [공통 응답 코드](#응답-코드) 참조

---

### 3.4 게시글 수정
**Endpoint:** `PATCH /posts/{postId}`

**헤더:** Authorization: Bearer {access_token}

**Request:** `{ "title": "...", "content": "...", "image_id": 1 }`

**선택:** title(String), content(String), image_id(Number)  
**참고:** PATCH는 부분 업데이트, 최소 1개 필드 필요 , 변경이 없을 경우 WAS 내에서 처리바람.

**응답:**
- 200: `update_post_success` → 수정된 정보 반환
- 403: `not_authorized` (작성자가 아님)
- 404: `post_not_found`
- 기타: [공통 응답 코드](#응답-코드) 참조

---

### 3.5 게시글 삭제
**Endpoint:** `DELETE /posts/{postId}` or `PUT /posts/{postId}`

**헤더:** Authorization: Bearer {access_token}

**Request:** `{ "post_status": "DELETED" }`

**필수:** post_status(String) - "DELETED" (DDL: ACTIVE, DELETED, DRAFT)

**응답:**
- 204: 삭제 성공 (응답 body 없음)
- 403: `not_authorized`
- 404: `post_not_found`
- 기타: [공통 응답 코드](#응답-코드) 참조

---

## 4. 이미지 (Images)

### 4.1 이미지 업로드
**Endpoint:** `POST /images`

**헤더:** Authorization: Bearer {access_token}, Content-Type: multipart/form-data

**Request:** file(File) - 업로드할 이미지 파일

**제약:** JPG/PNG/GIF, 최대 5MB

**응답:**
- 201: `upload_image_success` → image_id, image_url 반환
- 413: `file_too_large`
- 기타: [공통 응답 코드](#응답-코드) 참조

---

## 5. 댓글 (Comments)

**댓글 객체:** `{ comment_id, content, created_at, updated_at, author: { user_id, nickname, profile_image } }`

### 5.1 댓글 목록 조회
**Endpoint:** `GET /posts/{postId}/comments?offset=0&limit=10`

**쿼리:** offset(Number), limit(Number)

**응답:**
- 200: `get_comments_success` → comments[], pagination.total_count
- 404: `post_not_found`
- 기타: [공통 응답 코드](#응답-코드) 참조

---

### 5.2 댓글 작성
**Endpoint:** `POST /posts/{postId}/comments`

**헤더:** Authorization: Bearer {access_token}

**Request:** `{ "comment": "..." }`

**필수:** comment(String) - 200자 제한

**응답:**
- 201: `create_comment_success` → commentId, comment, author 반환
- 404: `post_not_found`
- 기타: [공통 응답 코드](#응답-코드) 참조

---

### 5.3 댓글 수정
**Endpoint:** `PATCH /posts/{postId}/comments/{commentId}`

**헤더:** Authorization: Bearer {access_token}

**Request:** `{ "comment": "..." }`

**필수:** comment(String) - 200자 제한

**응답:**
- 200: `update_comment_success` → 수정된 댓글 정보 반환
- 403: `not_authorized`
- 404: `post_not_found`
- 기타: [공통 응답 코드](#응답-코드) 참조

---

### 5.4 댓글 삭제
**Endpoint:** `PATCH /posts/{postId}/comments/{commentId}`

**헤더:** Authorization: Bearer {access_token}

**Request:** `{ "comment_status": "DELETED" }`

**필수:** comment_status(String) - "DELETED" (DDL: ACTIVE, DELETED)

**Note:** Soft delete 방식으로 PATCH 사용

**응답:**
- 204: 삭제 성공 (응답 body 없음)
- 403: `not_authorized`
- 404: `post_not_found`
- 기타: [공통 응답 코드](#응답-코드) 참조

---

## 6. 좋아요 (Likes)

### 6.1 게시글 좋아요
**Endpoint:** `POST /posts/{postId}/like`

**헤더:** Authorization: Bearer {access_token}

**응답:**
- 200: `like_success` → like count 반환
- 404: `post_not_found`
- 409: `already_liked`
- 기타: [공통 응답 코드](#응답-코드) 참조

---

### 6.2 게시글 좋아요 취소
**Endpoint:** `DELETE /posts/{postId}/like`

**헤더:** Authorization: Bearer {access_token}

**응답:**
- 200: `unlike_success` → like count 반환
- 404: `post_not_found`
- 기타: [공통 응답 코드](#응답-코드) 참조

---

### 6.3 내가 좋아요 한 게시글 목록 조회
**Endpoint:** `GET /users/me/likes?offset=0&limit=10`

**헤더:** Authorization: Bearer {access_token}

**쿼리:** offset(Number), limit(Number)

**응답:**
- 200: `get_liked_posts_success` → posts[], pagination.total_count
- 기타: [공통 응답 코드](#응답-코드) 참조

---

## 7. 공통 사양

### 인증 헤더
```
Authorization: Bearer {access_token}
```

### 페이지네이션
```
?offset=0&limit=10
```
offset: 시작 위치 (0부터), limit: 한 번에 가져올 개수

### 표준 응답 형식
```json
{
  "message": "작업_결과_메시지",
  "data": { /* 응답 데이터 또는 null */ },
  "timestamp": "2025-10-01T14:30:00"
}
```

### 응답 코드

**HTTP 상태 코드**
- 200: OK (요청 성공)
- 201: Created (리소스 생성 성공)
- 204: No Content (성공, body 없음)
- 400: Bad Request (입력 데이터 검증 실패)
- 401: Unauthorized (인증 실패)
- 403: Forbidden (권한 없음)
- 404: Not Found (리소스 없음)
- 409: Conflict (리소스 충돌)
- 413: Payload Too Large (파일 크기 초과)
- 429: Too Many Requests (Rate Limit)
- 500: Internal Server Error (서버 오류)

**공통 메시지 코드**
- `input_data_validation_failed` (400): 입력 데이터 검증 실패
- `token_not_valid` (401): 유효하지 않은 토큰
- `not_authorized` (403): 권한 없음
- `resource_not_found` (404): 리소스를 찾을 수 없음
- `post_not_found` (404): 게시글을 찾을 수 없음
- `resource_already_exists` (409): 리소스 중복
- `too_many_requests` (429): 요청 횟수 초과
- `internal_server_error` (500): 서버 내부 오류

### 응답 예시

**성공 예시:**
```json
{
  "message": "login_success",
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "timestamp": "2025-10-01T14:30:00"
}
```

**오류 예시:**
```json
{
  "message": "resource_already_exists",
  "data": {
    "field": ["email"]
  },
  "timestamp": "2025-10-01T14:30:00"
}
```