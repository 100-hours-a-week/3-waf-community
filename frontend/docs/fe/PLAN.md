# Express.js 정적 파일 서빙 서버 구축 가이드

## 문서 정보

| 항목 | 내용 |
|------|------|
| 작성일 | 2025-10-20 |
| 버전 | 2.0 |
| 목적 | Spring Boot SSR → Express.js 정적 파일 서버 분리 |
| 범위 | 프론트엔드 정적 파일 서빙만 (백엔드는 Spring Boot 유지) |

**참조 문서**:
- **⭐ [API.md](../be/API.md)**: REST API 명세 - Spring Boot 백엔드 엔드포인트
- **⭐ [LLD.md](../be/LLD.md)**: 백엔드 설계 문서 - Spring Boot 구현 패턴
- **[FRONTEND_GUIDE.md](./FRONTEND_GUIDE.md)**: API 연동 가이드 - 클라이언트 구현

---

## 프로젝트 개요

### 목적
기존 Spring Boot 프로젝트의 **SSR(Server-Side Rendering) 부분만 Express.js 정적 파일 서버로 분리**하여 프론트엔드/백엔드 개발 독립성 확보

### 아키텍처

**분리 전 (기존)**:
```
Client ← Spring Boot (SSR + API + Database)
```

**분리 후 (목표)**:
```
Client
  ↓ (정적 파일 요청)
Express.js Static File Server (port 3000)
  ↓ (REST API 호출)
Spring Boot Backend API (port 8080)
  ↓
MySQL Database
```

### 범위 명확화

| 항목 | 담당 서버 | 상태 |
|------|----------|------|
| HTML/CSS/JS 서빙 | Express.js | ✅ 완료 |
| REST API | Spring Boot | ✅ 유지 (변경 없음) |
| JWT 인증 | Spring Boot | ⚠️ HttpOnly Cookie 전환 중 |
| Database | Spring Boot + MySQL | ✅ 유지 (변경 없음) |
| S3 이미지 업로드 | Spring Boot | ✅ 유지 (변경 없음) |

**중요**: 백엔드 로직, 데이터베이스, 비즈니스 규칙은 **모두 Spring Boot 유지**

---

## Phase 1: Express.js 정적 파일 서버 구축 ✅ 완료

### 목표
`origin_source/static/` 디렉토리의 HTML, CSS, JavaScript 파일을 Express.js로 서빙

### 작업 내용

#### 1.1 프로젝트 초기화
```bash
# Node.js 프로젝트 초기화
npm init -y

# Express.js 설치
npm install express

# 개발 도구 설치
npm install --save-dev nodemon
```

#### 1.2 server.js 작성
```javascript
// server.js
const express = require('express');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

// 정적 파일 서빙 설정
app.use(express.static(path.join(__dirname, 'origin_source/static')));

// SPA 라우팅 폴백 (선택)
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'origin_source/static/pages/index.html'));
});

app.listen(PORT, () => {
  console.log(`✅ Express.js static file server running on http://localhost:${PORT}`);
  console.log(`📁 Serving files from: origin_source/static/`);
});
```

#### 1.3 package.json 스크립트
```json
{
  "scripts": {
    "start": "node server.js",
    "dev": "nodemon server.js"
  }
}
```

#### 1.4 실행 확인
```bash
# 개발 모드 (자동 재시작)
npm run dev

# 일반 모드
npm start

# 접속 테스트
curl http://localhost:3000/pages/user/login.html
```

### 완료 조건
- [x] Express.js 서버 실행 성공 (port 3000)
- [x] HTML/CSS/JS 파일 정상 서빙
- [x] `origin_source/static/` 경로 접근 가능
- [x] 개발 모드 (nodemon) 작동

---

## Phase 2: CORS 설정 및 API 연동 확인

### 목표
Express.js에서 서빙된 프론트엔드가 Spring Boot API (port 8080)와 정상 통신

### Spring Boot CORS 설정 확인

#### 2.1 SecurityConfig.java 확인
```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // ... 나머지 설정
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000"  // Express.js 정적 파일 서버
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);  // 중요: Cookie 전송 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

**핵심 설정**:
- `allowedOrigins`: `http://localhost:3000` 추가
- `allowCredentials(true)`: HttpOnly Cookie 전송 허용

#### 2.2 프론트엔드 API 호출 테스트
```javascript
// origin_source/static/js/common/api.js
const API_BASE_URL = 'http://localhost:8080';

async function testApiConnection() {
  try {
    const response = await fetch(`${API_BASE_URL}/posts?limit=5`, {
      method: 'GET',
      credentials: 'include'  // 중요: Cookie 전송
    });

    if (response.ok) {
      console.log('✅ API 연동 성공');
      const data = await response.json();
      console.log('게시글 목록:', data);
    } else {
      console.error('❌ API 연동 실패:', response.status);
    }
  } catch (error) {
    console.error('❌ 네트워크 오류:', error);
  }
}

// 브라우저 콘솔에서 테스트
testApiConnection();
```

### 완료 조건
- [x] Spring Boot CORS 설정에 `http://localhost:3000` 추가 (✅ 백엔드 완료 2025-10-20)
- [x] `allowCredentials(true)` 설정 (Cookie 전송) (✅ 백엔드 완료 2025-10-20)
- [ ] 프론트엔드 → Spring Boot API 호출 성공 (⏳ 수동 테스트 필요)
- [ ] CORS 에러 없음 (Network 탭 확인) (⏳ 수동 테스트 필요)

---

## Phase 3: JWT HttpOnly Cookie 전환 ✅ 코드 완료 (테스트 대기)

### 목표
localStorage JWT 토큰을 HttpOnly Cookie로 전환하여 XSS 공격 방어

### Before/After 비교

**현재 (localStorage)**:
```javascript
localStorage.setItem('access_token', token);  // ❌ XSS 취약
fetch(url, { headers: { 'Authorization': `Bearer ${token}` } });
```

**목표 (HttpOnly Cookie)**:
```javascript
// 토큰 저장/조회 로직 삭제
fetch(url, { credentials: 'include' });  // ✅ Cookie 자동 전송
```

### 목표 상태 (HttpOnly Cookie)

#### 3.1 Spring Boot 백엔드 (핵심 변경)

**Cookie 설정 (AuthController.java)**:
```java
// ✅ 응답 시 HttpOnly Cookie 설정
Cookie cookie = new Cookie("access_token", token);
cookie.setHttpOnly(true);  // XSS 방지
cookie.setSecure(true);    // HTTPS only
cookie.setAttribute("SameSite", "Lax");  // CSRF 방지
response.addCookie(cookie);
```

**토큰 추출 (JwtAuthenticationFilter.java)**:
```java
// ✅ Cookie에서 토큰 추출
String token = Arrays.stream(request.getCookies())
    .filter(c -> "access_token".equals(c.getName()))
    .map(Cookie::getValue)
    .findFirst()
    .orElse(null);
```

#### 3.2 프론트엔드 (핵심 변경)

**api.js - credentials 추가**:
```javascript
// ❌ 삭제
localStorage.setItem('access_token', ...);
Authorization: `Bearer ${token}`

// ✅ 추가
fetch(url, {
  credentials: 'include',  // Cookie 자동 전송
  headers: { 'Content-Type': 'application/json' }
});
```

**login.js - 토큰 저장 로직 제거**:
```javascript
// ❌ 삭제
localStorage.setItem('access_token', data.data.access_token);

// ✅ 변경: 서버가 Cookie 설정, 바로 리다이렉트
window.location.href = '/pages/board/list.html';
```

### 보안 강화 효과

| 공격 유형 | localStorage (기존) | HttpOnly Cookie (목표) |
|---------|--------------------|-----------------------|
| **XSS** | ❌ 토큰 탈취 가능 | ✅ JavaScript 접근 차단 |
| **CSRF** | ✅ 안전 (SOP) | ✅ SameSite=Lax 설정 |
| **Network 감청** | ⚠️ HTTPS 필요 | ✅ Secure 플래그 |

### 완료 조건
- [x] Spring Boot: Cookie 기반 JWT 발급 구현 (✅ 백엔드 완료 2025-10-20)
- [x] Spring Boot: JwtAuthenticationFilter Cookie 추출 구현 (✅ 백엔드 완료 2025-10-20)
- [x] 프론트엔드: localStorage 토큰 로직 제거 (✅ 프론트엔드 완료 2025-10-21)
- [x] 프론트엔드: 모든 API 호출에 `credentials: 'include'` 추가 (✅ 프론트엔드 완료 2025-10-21)
- [ ] 로그인/로그아웃/토큰 갱신 테스트 통과 (⏳ 수동 테스트 필요)
- [ ] Chrome DevTools → Application → Cookies 확인 (⏳ 수동 테스트 필요)

---

## Phase 4: 통합 테스트 및 검증

### 목표
Express.js + Spring Boot 통합 환경에서 전체 기능 검증

### 테스트 시나리오

#### 4.1 인증 플로우
1. 로그인 → Chrome DevTools → Application → Cookies 확인
2. 게시글 목록 접근 (Cookie 자동 전송 확인)
3. 로그아웃 (Cookie 삭제 확인)

#### 4.2 API 연동 (브라우저 콘솔)
```javascript
// GET /posts (credentials 전송 확인)
fetch('http://localhost:8080/posts?limit=5', { credentials: 'include' })
  .then(r => r.json()).then(console.log);

// POST /posts (인증 확인)
fetch('http://localhost:8080/posts', {
  method: 'POST',
  credentials: 'include',
  body: JSON.stringify({ title: '테스트', content: '내용' })
}).then(r => r.json()).then(console.log);
```

#### 4.3 에러 처리
- 401: Cookie 삭제 → API 호출 → 자동 갱신 확인
- 403: 타인 게시글 수정 → "POST-002" 확인
- CORS: Network 탭 → Preflight 요청 확인

### 완료 조건
- [ ] 로그인/로그아웃 정상 작동
- [ ] 게시글 CRUD 정상 작동
- [ ] 댓글 CRUD 정상 작동
- [ ] 좋아요 기능 정상 작동
- [ ] 이미지 업로드 정상 작동
- [ ] 401 에러 시 자동 토큰 갱신
- [ ] CORS 에러 없음

---

## Phase 5: 배포 준비 및 최적화

### 목표
운영 환경 배포를 위한 설정 및 최적화

### 5.1 환경 변수 분리

**.env.development / .env.production**:
```bash
PORT=3000
NODE_ENV=development  # production
API_BASE_URL=http://localhost:8080  # https://api.yourdomain.com
```

**server.js**:
```javascript
require('dotenv').config();
const PORT = process.env.PORT || 3000;
```

### 5.2 Nginx 리버스 프록시 (선택)

```nginx
location / { proxy_pass http://localhost:3000; }  # Express.js
location /api/ { proxy_pass http://localhost:8080/; }  # Spring Boot
```

### 5.3 Docker (선택)

```dockerfile
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
EXPOSE 3000
CMD ["npm", "start"]
```

**docker-compose.yml**:
```yaml
services:
  frontend:
    build: .
    ports: ["3000:3000"]
  backend:
    image: spring-boot:latest
    ports: ["8080:8080"]
```

### 완료 조건
- [ ] 환경 변수 분리 (.env.development, .env.production)
- [ ] 운영 환경에서 HTTPS 설정 (Nginx 또는 AWS ALB)
- [ ] Secure Cookie 플래그 활성화 (운영 환경만)
- [ ] 정적 파일 캐싱 설정 (선택)
- [ ] Docker 이미지 빌드 성공 (선택)

---

## 참고 사항

### Spring Boot 백엔드 변경 없음
**중요**: 이 가이드는 프론트엔드 서빙 방식 변경만 다룹니다. Spring Boot 백엔드는 **변경하지 않습니다**.

- **유지**: 비즈니스 로직, 데이터베이스, JWT 생성, S3 업로드
- **변경**: JWT 전달 방식만 (Authorization 헤더 → Cookie)

### CORS 설정 체크리스트
- [ ] `allowedOrigins`: `http://localhost:3000` 추가
- [ ] `allowCredentials(true)`: Cookie 전송 허용
- [ ] `allowedMethods`: GET, POST, PUT, PATCH, DELETE, OPTIONS
- [ ] `allowedHeaders`: `*` (또는 명시적 헤더 목록)

### HttpOnly Cookie 설정 체크리스트
- [ ] `httpOnly`: true (JavaScript 접근 차단)
- [ ] `secure`: true (HTTPS only, 운영 환경)
- [ ] `sameSite`: Lax (CSRF 방어)
- [ ] `path`: `/` (전체 경로)
- [ ] `maxAge`: Access 30분, Refresh 7일

### 문제 해결

| 에러 | 원인 | 해결 |
|------|------|------|
| CORS 차단 | `allowedOrigins("*")` | `allowedOrigins("http://localhost:3000")` |
| Cookie 미전송 | `credentials` 누락 | `credentials: 'include'` 추가 |
| Secure 에러 | 개발 환경 HTTPS 없음 | 개발: `secure: false`, 운영: `secure: true` |

---

## 진행 상황 요약

| Phase | 작업 내용 | 상태 | 완료일 |
|-------|----------|------|--------|
| Phase 1 | Express.js 정적 파일 서버 구축 | ✅ 완료 | 2025-10-20 |
| Phase 2 | CORS 설정 및 API 연동 확인 | ⚠️ 코드 완료, 테스트 대기 | 2025-10-20 |
| Phase 3 | JWT HttpOnly Cookie 전환 | ✅ 코드 완료, 테스트 대기 | 2025-10-21 |
| Phase 4 | 통합 테스트 및 검증 | ⏳ 대기 | - |
| Phase 5 | 배포 준비 및 최적화 | ⏳ 대기 | - |

**현재 진행률**: Phase 1 완료 (100%), Phase 2 코드 완료 (80%), Phase 3 코드 완료 (90%)
**다음 단계**: Express.js + Spring Boot 서버 실행 → Phase 2-3 통합 수동 테스트 (30-40분) → Phase 4 진입

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 |
|------|------|-----------|
| 2025-10-20 | 1.0 | Spring Boot → Express.js 마이그레이션 계획 (삭제됨) |
| 2025-10-20 | 2.0 | Express.js 정적 파일 서빙 서버 구축 가이드로 전면 재작성 |
| 2025-10-20 | 2.1 | 코드 예제 간소화 (711줄 → 440줄, 38% 감소) - 핵심 스니펫만 유지 |
| 2025-10-21 | 2.2 | Phase 3 프론트엔드 작업 완료 (localStorage 제거, credentials 추가) - 진행률 67% |
| 2025-10-21 | 2.3 | Phase 2 백엔드 CORS 설정 검증 완료 - Phase 2/3 통합 테스트 대기 |
