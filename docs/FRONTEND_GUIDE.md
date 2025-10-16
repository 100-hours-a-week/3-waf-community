# 프론트엔드 연동 가이드

**버전**: 1.0
**Base URL**: `http://localhost:8080`
**상세 API 스펙**: [API.md](API.md) 참조

---

## 목차

1. [시작하기](#1-시작하기)
2. [인증 시스템](#2-인증-시스템)
3. [페이지네이션 전략](#3-페이지네이션-전략)
4. [파일 업로드](#4-파일-업로드)
5. [입력 검증](#5-입력-검증)
6. [에러 처리](#6-에러-처리)
7. [개발 팁](#7-개발-팁)

---

## 1. 시작하기

### 1.1 Quick Start

```javascript
// 로그인
const response = await fetch('http://localhost:8080/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'test@example.com',
    password: 'Test1234!'
  })
});

const data = await response.json();
// { message: "login_success", data: { access_token, refresh_token }, timestamp }

// 토큰 저장
localStorage.setItem('accessToken', data.data.access_token);
localStorage.setItem('refreshToken', data.data.refresh_token);

// 인증 API 호출
const posts = await fetch('http://localhost:8080/posts', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
  }
});
```

### 1.2 공통 헤더

| 상황 | Content-Type | Authorization |
|------|--------------|---------------|
| JSON 요청 | application/json | - |
| 인증 필요 | application/json | Bearer {token} |
| 파일 업로드 | multipart/form-data | Bearer {token} |

### 1.3 응답 구조

**성공**:
```json
{
  "message": "작업_결과_메시지",
  "data": { /* 실제 데이터 */ },
  "timestamp": "2025-10-16T10:00:00"
}
```

**실패**:
```json
{
  "message": "ERROR-CODE",
  "data": { "details": "상세 메시지" },
  "timestamp": "2025-10-16T10:00:00"
}
```

---

## 2. 인증 시스템

### 2.1 토큰 관리

**토큰 종류**:
- **Access Token**: 30분, API 요청 시 사용
- **Refresh Token**: 7일, Access Token 갱신용

**발급 시점**: 회원가입(`POST /users/signup`), 로그인(`POST /auth/login`)

### 2.2 토큰 갱신 자동화

```javascript
// fetchWithAuth 헬퍼 함수
async function fetchWithAuth(url, options = {}) {
  let accessToken = localStorage.getItem('accessToken');

  // 첫 시도
  let response = await fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      'Authorization': `Bearer ${accessToken}`
    }
  });

  // 401 에러 시 토큰 갱신 후 재시도
  if (response.status === 401) {
    const refreshToken = localStorage.getItem('refreshToken');
    const refreshResponse = await fetch('http://localhost:8080/auth/refresh_token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refresh_token: refreshToken })
    });

    if (refreshResponse.ok) {
      const newTokens = await refreshResponse.json();
      accessToken = newTokens.data.access_token;
      localStorage.setItem('accessToken', accessToken);

      // 재시도
      response = await fetch(url, {
        ...options,
        headers: {
          ...options.headers,
          'Authorization': `Bearer ${accessToken}`
        }
      });
    } else {
      // Refresh Token도 만료 → 재로그인
      window.location.href = '/login';
    }
  }

  return response;
}
```

### 2.3 주요 API

| 엔드포인트 | 메서드 | 인증 | 설명 |
|-----------|--------|------|------|
| /auth/login | POST | ❌ | 로그인 |
| /auth/logout | POST | ❌ | 로그아웃 (refresh_token 필요) |
| /auth/refresh_token | POST | ❌ | Access Token 갱신 |

상세 스펙: [API.md Section 1](API.md#1-인증-authentication)

---

## 3. 페이지네이션 전략

### 3.1 두 가지 방식

| 방식 | 사용처 | 파라미터 | 응답 필드 |
|------|--------|----------|-----------|
| **Cursor** | 게시글 최신순 | `cursor`, `limit` | `nextCursor`, `hasMore` |
| **Offset** | 좋아요순, 댓글 | `offset`, `limit` | `total_count` |

⚠️ **Breaking Change**: 게시글 최신순(`sort=latest`)은 **offset 파라미터를 지원하지 않습니다**

### 3.2 Cursor 기반 (무한 스크롤)

**엔드포인트**: `GET /posts?sort=latest&limit=10&cursor={cursor}`

**React 구현 예시**:
```javascript
function PostList() {
  const [posts, setPosts] = useState([]);
  const [cursor, setCursor] = useState(null);
  const [hasMore, setHasMore] = useState(true);

  const loadMore = async () => {
    const url = cursor
      ? `http://localhost:8080/posts?sort=latest&limit=10&cursor=${cursor}`
      : 'http://localhost:8080/posts?sort=latest&limit=10';

    const response = await fetch(url);
    const data = await response.json();

    setPosts([...posts, ...data.data.posts]);
    setCursor(data.data.nextCursor);  // 다음 cursor
    setHasMore(data.data.hasMore);    // 더 있는지 확인
  };

  return (
    <div>
      {posts.map(post => <PostItem key={post.postId} post={post} />)}
      {hasMore && <button onClick={loadMore}>더 보기</button>}
    </div>
  );
}
```

**특징**:
- `nextCursor`가 null이면 마지막 페이지
- `hasMore`가 false면 끝
- **total_count 제공 안 함**

### 3.3 Offset 기반 (페이지 번호)

**엔드포인트**: `GET /posts?sort=likes&offset=0&limit=10`

**React 구현 예시**:
```javascript
function PostListWithPagination() {
  const [posts, setPosts] = useState([]);
  const [totalCount, setTotalCount] = useState(0);
  const [page, setPage] = useState(1);
  const limit = 10;

  useEffect(() => {
    const offset = (page - 1) * limit;
    fetch(`http://localhost:8080/posts?sort=likes&offset=${offset}&limit=${limit}`)
      .then(res => res.json())
      .then(data => {
        setPosts(data.data.posts);
        setTotalCount(data.data.pagination.total_count);
      });
  }, [page]);

  const totalPages = Math.ceil(totalCount / limit);

  return (
    <div>
      {posts.map(post => <PostItem key={post.postId} post={post} />)}
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </div>
  );
}
```

**특징**:
- `total_count` 제공 (페이지 번호 계산 가능)
- offset 증가로 다음 페이지 요청

### 3.4 적용 대상

| API | 방식 | 이유 |
|-----|------|------|
| GET /posts?sort=latest | **Cursor** | 무한 스크롤, 실시간 안정성 |
| GET /posts?sort=likes | Offset | 페이지 번호 필요 (추후 cursor 예정) |
| GET /posts/{postId}/comments | Offset | 페이지 번호 필요 |
| GET /posts/users/me/likes | Offset | 페이지 번호 필요 (추후 cursor 예정) |

---

## 4. 파일 업로드

### 4.1 Multipart 요청

**사용처**: 회원가입, 프로필 수정, 이미지 업로드

**제약사항**:
- 파일 형식: JPG, PNG, GIF
- 최대 크기: 5MB
- 검증: 서버 측 MIME type + Magic Number

### 4.2 회원가입 예시

```javascript
const formData = new FormData();
formData.append('email', 'test@example.com');
formData.append('password', 'Test1234!');
formData.append('nickname', '테스트유저');

// 프로필 이미지 (선택)
if (fileInput.files[0]) {
  formData.append('profile_image', fileInput.files[0]);
}

const response = await fetch('http://localhost:8080/users/signup', {
  method: 'POST',
  body: formData  // Content-Type 자동 설정
});

// 에러 처리
if (!response.ok) {
  const error = await response.json();

  if (error.message === 'IMAGE-002') {
    alert('파일 크기는 5MB 이하여야 합니다.');
  } else if (error.message === 'IMAGE-003') {
    alert('JPG, PNG, GIF 파일만 업로드 가능합니다.');
  }
}
```

### 4.3 이미지 단독 업로드 (게시글용)

```javascript
// 1단계: 이미지 업로드
const formData = new FormData();
formData.append('file', fileInput.files[0]);

const imageResponse = await fetchWithAuth('http://localhost:8080/images', {
  method: 'POST',
  body: formData
});

const { imageId, imageUrl } = (await imageResponse.json()).data;

// 2단계: 게시글 작성 시 imageId 사용
const postResponse = await fetchWithAuth('http://localhost:8080/posts', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    title: '게시글 제목',
    content: '게시글 내용',
    image_id: imageId  // 업로드된 이미지 ID
  })
});
```

---

## 5. 입력 검증

### 5.1 필드별 제약

| 필드 | 제약 | 검증 위치 |
|------|------|-----------|
| 이메일 | 유효한 형식, 중복 불가 | 프론트 + 백엔드 |
| 비밀번호 | 8-20자, 대소문자/특수문자 각 1개+ | 프론트 + 백엔드 |
| 닉네임 | 최대 10자, 중복 불가 | 프론트 + 백엔드 |
| 게시글 제목 | 최대 27자 | 프론트 + 백엔드 |
| 댓글 | 최대 200자 | 프론트 + 백엔드 |
| 이미지 | JPG/PNG/GIF, 5MB | 프론트 + 백엔드 |

### 5.2 비밀번호 정책

```javascript
const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?":{}|<>]).{8,20}$/;

function validatePassword(password) {
  if (!PASSWORD_REGEX.test(password)) {
    return '비밀번호는 8-20자이며, 대문자, 소문자, 특수문자를 각각 1개 이상 포함해야 합니다.';
  }
  return null;
}
```

### 5.3 프론트엔드 검증 함수

```javascript
function validateSignupForm(formData) {
  const errors = {};

  // 이메일
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(formData.email)) {
    errors.email = '유효한 이메일 주소를 입력하세요.';
  }

  // 비밀번호
  const passwordError = validatePassword(formData.password);
  if (passwordError) errors.password = passwordError;

  // 닉네임
  if (formData.nickname.length > 10) {
    errors.nickname = '닉네임은 최대 10자입니다.';
  }

  // 파일
  if (formData.profileImage) {
    const allowedTypes = ['image/jpeg', 'image/png', 'image/gif'];
    if (!allowedTypes.includes(formData.profileImage.type)) {
      errors.profileImage = 'JPG, PNG, GIF 파일만 가능합니다.';
    }

    const maxSize = 5 * 1024 * 1024; // 5MB
    if (formData.profileImage.size > maxSize) {
      errors.profileImage = '파일 크기는 5MB 이하여야 합니다.';
    }
  }

  return Object.keys(errors).length > 0 ? errors : null;
}
```

---

## 6. 에러 처리

### 6.1 주요 에러 코드

| 코드 | HTTP | 설명 | 처리 방법 |
|------|------|------|----------|
| AUTH-001 | 401 | 잘못된 이메일/비밀번호 | 재입력 유도 |
| AUTH-003 | 401 | Access Token 만료 | 자동 갱신 |
| AUTH-004 | 401 | Refresh Token 만료 | 재로그인 |
| USER-002 | 409 | 이메일 중복 | 다른 이메일 유도 |
| USER-003 | 409 | 닉네임 중복 | 다른 닉네임 유도 |
| USER-004 | 400 | 비밀번호 정책 위반 | 정책 안내 |
| POST-002 | 403 | 작성자 불일치 | 권한 안내 |
| COMMENT-002 | 403 | 작성자 불일치 | 권한 안내 |
| LIKE-001 | 409 | 이미 좋아요함 | 무시 또는 취소 유도 |
| IMAGE-002 | 413 | 파일 크기 초과 | 5MB 이하 안내 |
| IMAGE-003 | 400 | 유효하지 않은 파일 형식 | JPG/PNG/GIF 안내 |
| COMMON-004 | 429 | Rate Limit | 재시도 (지수 백오프) |

**전체 에러 코드**: [API.md Section 7](API.md#7-공통-사양) 참조

### 6.2 에러 핸들러 구현

```javascript
async function handleApiRequest(url, options) {
  try {
    const response = await fetch(url, options);
    const data = await response.json();

    if (!response.ok) {
      switch (data.message) {
        case 'AUTH-001':
          alert('이메일 또는 비밀번호가 잘못되었습니다.');
          break;
        case 'USER-002':
          alert('이미 사용 중인 이메일입니다.');
          break;
        case 'USER-003':
          alert('이미 사용 중인 닉네임입니다.');
          break;
        case 'POST-002':
        case 'COMMENT-002':
          alert('작성자만 수정/삭제할 수 있습니다.');
          break;
        case 'IMAGE-002':
          alert('파일 크기는 5MB 이하여야 합니다.');
          break;
        case 'COMMON-004':
          alert('요청이 너무 많습니다. 잠시 후 다시 시도하세요.');
          break;
        default:
          alert(`오류: ${data.data.details}`);
      }
      throw new Error(data.message);
    }

    return data;
  } catch (error) {
    console.error('API 요청 실패:', error);
    throw error;
  }
}
```

### 6.3 Rate Limit (429) 재시도

```javascript
async function fetchWithRetry(url, options, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    const response = await fetch(url, options);

    if (response.status === 429) {
      const delay = Math.pow(2, i) * 1000; // 1초, 2초, 4초
      console.log(`Rate limited. Retrying in ${delay}ms...`);
      await new Promise(resolve => setTimeout(resolve, delay));
      continue;
    }

    return response;
  }
}
```

---

## 7. 개발 팁

### 7.1 JWT 디코딩

```javascript
// Access Token 디코딩 (만료 시간 확인)
function parseJwt(token) {
  const base64Url = token.split('.')[1];
  const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
  const jsonPayload = decodeURIComponent(
    atob(base64)
      .split('')
      .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
      .join('')
  );
  return JSON.parse(jsonPayload);
}

const token = localStorage.getItem('accessToken');
const payload = parseJwt(token);
console.log('Token expires at:', new Date(payload.exp * 1000));
```

### 7.2 개발 체크리스트

#### 회원가입 구현 전
- [ ] 비밀번호 정책 검증 구현
- [ ] 이메일 형식 검증
- [ ] 닉네임 길이 제한 (10자)
- [ ] 프로필 이미지 파일 검증 (5MB, JPG/PNG/GIF)

#### 게시글 목록 구현 전
- [ ] 최신순(cursor)과 좋아요순(offset) 페이지네이션 구분
- [ ] 무한 스크롤 vs 페이지 번호 UI 결정

#### 인증 구현 전
- [ ] Access Token 저장 방법 결정 (localStorage vs httpOnly cookie)
- [ ] 토큰 갱신 로직 구현 (fetchWithAuth)
- [ ] 401 에러 시 재로그인 플로우

### 7.3 Postman Collection

**환경 변수 설정**:
```json
{
  "base_url": "http://localhost:8080",
  "access_token": "{{login_response.access_token}}",
  "refresh_token": "{{login_response.refresh_token}}"
}
```

**자동 토큰 갱신 (Pre-request Script)**:
```javascript
// Access Token 만료 시 자동 갱신
const expiresAt = pm.environment.get('token_expires_at');
if (Date.now() > expiresAt) {
  pm.sendRequest({
    url: pm.environment.get('base_url') + '/auth/refresh_token',
    method: 'POST',
    body: {
      mode: 'raw',
      raw: JSON.stringify({
        refresh_token: pm.environment.get('refresh_token')
      })
    }
  }, (err, res) => {
    pm.environment.set('access_token', res.json().data.access_token);
  });
}
```

---

## FAQ

**Q1. Access Token이 만료되면?**
A1. 401 에러 발생 시 Refresh Token으로 갱신 후 재시도 (Section 2.2 참조)

**Q2. 페이지네이션 방식이 왜 두 가지?**
A2. 최신순은 무한 스크롤(cursor), 좋아요순은 페이지 번호(offset) 필요 (Section 3 참조)

**Q3. 회원가입 시 이미지 필수?**
A3. 선택 사항. `profile_image` 생략 가능

**Q4. Rate Limit에 걸리면?**
A4. 429 응답 시 1-2초 대기 후 재시도 (Section 6.3 참조)

---

## 참고 문서

- **전체 API 스펙**: [API.md](API.md)
- **에러 코드 전체**: [API.md Section 7](API.md#7-공통-사양)
- **DB 스키마**: [DDL.md](DDL.md)
- **설계 문서**: [LLD.md](LLD.md)

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 |
|------|------|-----------|
| 2025-10-16 | 1.0 | 프론트엔드 연동 가이드 작성 (간소화) |
