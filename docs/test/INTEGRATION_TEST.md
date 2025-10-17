# 프론트엔드-백엔드 연동 테스트 가이드

## 1. 사전 준비

### 1.1 필수 환경 변수

환경 변수는 `.env` 파일로 관리됩니다 (Spring Boot dotenv 자동 로드)

프로젝트 루트에 `.env` 파일이 이미 준비되어 있습니다:
```bash
DB_URL=jdbc:mysql://localhost:3306/community
DB_USERNAME=root
DB_PASSWORD=your_password

JWT_SECRET=your-256bit-secret-key-here-minimum-32-characters-required

# AWS S3 (Phase 3.5+)
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_REGION=ap-northeast-2
AWS_S3_BUCKET=ktb-3-community-images-dev
```

**참고**: Spring Boot가 dotenv를 통해 자동으로 로드하므로 export 명령어 불필요

### 1.2 데이터베이스

**데이터베이스는 다른 담당자가 관리합니다 (이미 준비됨)**

테스트 시 DB 접근 확인만 필요:
```bash
# MySQL 접속 확인
mysql -u root -p -e "USE community; SHOW TABLES;"
# 8개 테이블 확인: users, posts, post_stats, comments, post_likes, images, post_images, user_tokens
```

### 1.3 백엔드 서버 실행

```bash
# Gradle 빌드 및 실행
./gradlew bootRun

# 또는 IDE에서 CommunityApplication.main() 실행
```

**서버 시작 확인:**
- 콘솔에 "Started CommunityApplication" 메시지 확인
- http://localhost:8080 접근 가능 확인

---

## 2. 테스트 시나리오

### Phase 1: 회원가입 및 로그인

**접속 경로:** http://localhost:8080/user/register.html

**테스트 항목:**
1. ✅ 회원가입 폼 렌더링
2. ✅ 이메일 중복 검증 (USER-002)
3. ✅ 닉네임 중복 검증 (USER-003)
4. ✅ 비밀번호 정책 검증 (8-20자, 대/소/특수문자)
5. ✅ 프로필 이미지 업로드 (선택, 5MB 제한)
6. ✅ 회원가입 성공 → 자동 로그인 → /board/list.html 리다이렉트

**테스트 데이터:**
```
이메일: test@startupcode.kr
비밀번호: Test1234!
닉네임: 테스터
프로필 이미지: (선택)
```

**로그인 테스트:** http://localhost:8080/user/login.html
1. ✅ 잘못된 비밀번호 → AUTH-001 에러 메시지 (토큰 갱신 시도 안함)
2. ✅ 로그인 성공 → access_token, refresh_token localStorage 저장
3. ✅ 게시글 목록 페이지로 리다이렉트

---

### Phase 2: 게시글 목록 및 상세

**접속 경로:** http://localhost:8080/board/list.html

**게시글 목록 테스트:**
1. ✅ 최신 10개 게시글 로드 (Cursor 페이지네이션)
2. ✅ 게시글 카드 렌더링 (제목, 작성자, 날짜, 통계)
3. ✅ 상대 시간 표시 ("방금 전", "5분 전", "2시간 전")
4. ✅ 축약형 숫자 표시 (1234 → "1.2k", 10000 → "10k")
5. ✅ 더보기 버튼 클릭 → 다음 페이지 로드
6. ✅ 프로필 이미지 로드 (로그인 상태)

**게시글 작성 버튼:**
1. ✅ 비로그인 → "로그인이 필요합니다" alert
2. ✅ 로그인 → /board/write.html 이동

**게시글 상세 테스트:** (카드 클릭)
1. ✅ 게시글 상세 정보 렌더링
2. ✅ 작성자 정보, 날짜, 조회수, 좋아요, 댓글 수
3. ✅ 이미지 표시 (있을 경우)
4. ✅ 본인 게시글 → 수정/삭제 버튼 표시

---

### Phase 3: 좋아요 및 댓글

**좋아요 테스트:**
1. ✅ 비로그인 → "로그인이 필요합니다"
2. ✅ 좋아요 추가 → 하트 아이콘 활성화, 카운트 증가
3. ✅ 좋아요 취소 → 하트 아이콘 비활성화, 카운트 감소
4. ✅ 중복 좋아요 → LIKE-001 에러

**댓글 작성:**
1. ✅ 비로그인 → "로그인이 필요합니다"
2. ✅ 댓글 작성 (200자 제한)
3. ✅ 댓글 목록 맨 위에 추가 (실시간)
4. ✅ 댓글 수 카운트 증가 (off-by-one 수정 확인)

**댓글 수정/삭제:**
1. ✅ 본인 댓글 → 수정/삭제 버튼 표시
2. ✅ 수정 버튼 → textarea에 내용 채워짐, "댓글 수정" 버튼
3. ✅ 수정 제출 → 내용 업데이트, "수정됨" 표시
4. ✅ 삭제 확인 → 댓글 제거, 카운트 감소

---

### Phase 4: 게시글 작성 및 수정

**게시글 작성:** http://localhost:8080/board/write.html

1. ✅ 로그인 확인 (비로그인 시 리다이렉트)
2. ✅ 제목 입력 (27자 제한, 실시간 검증)
3. ✅ 내용 입력 (필수)
4. ✅ 이미지 업로드 (선택)
   - 파일 크기 검증 (5MB)
   - 파일 형식 검증 (JPG/PNG/GIF)
   - 미리보기 표시
5. ✅ 작성완료 → POST /images → POST /posts
6. ✅ 게시글 상세 페이지로 리다이렉트

**게시글 수정:** (상세 페이지에서 수정 버튼)

1. ✅ 기존 데이터 로드 (제목, 내용, 이미지)
2. ✅ 이미지 유지/변경/삭제
3. ✅ 수정완료 → PATCH /posts/{id}
4. ✅ 게시글 상세 페이지로 리다이렉트

**게시글 삭제:**
1. ✅ 삭제 확인 모달
2. ✅ 삭제 → DELETE /posts/{id}
3. ✅ 게시글 목록 페이지로 리다이렉트

---

### Phase 5: 프로필 관리

**프로필 수정:** http://localhost:8080/user/profile-edit.html

1. ✅ 로그인 확인
2. ✅ 현재 프로필 정보 로드
3. ✅ 닉네임 변경 (10자 제한)
4. ✅ 프로필 이미지 변경 (multipart/form-data)
5. ✅ 저장 → PATCH /users/{id}
6. ✅ 페이지 새로고침

**회원 탈퇴:**
1. ✅ 이중 확인 ("정말 탈퇴하시겠습니까?", "복구 불가")
2. ✅ 탈퇴 → PUT /users/{id} (INACTIVE 상태)
3. ✅ 로그아웃 → 로그인 페이지로 리다이렉트

**비밀번호 변경:** http://localhost:8080/user/password-change.html

1. ✅ 현재 비밀번호 입력
2. ✅ 새 비밀번호 입력 (정책 검증)
3. ✅ 새 비밀번호 확인 (일치 확인)
4. ✅ 변경 → PATCH /users/{id}/password
5. ✅ 로그아웃 → "다시 로그인해주세요"

---

## 3. 에러 처리 테스트

### 3.1 인증 에러
- ✅ 401 토큰 만료 → 자동 토큰 갱신 시도
- ✅ 401 로그인 실패 (AUTH-001) → 즉시 에러 메시지, 토큰 갱신 안함
- ✅ 403 권한 없음 → "권한이 없습니다"

### 3.2 검증 에러
- ✅ USER-002: 이메일 중복
- ✅ USER-003: 닉네임 중복
- ✅ USER-004: 비밀번호 정책 위반
- ✅ USER-006: 현재 비밀번호 불일치
- ✅ IMAGE-002: 파일 크기 초과
- ✅ IMAGE-003: 잘못된 파일 형식

### 3.3 리소스 에러
- ✅ POST-001: 게시글 없음 (404)
- ✅ COMMENT-001: 댓글 없음 (404)
- ✅ LIKE-001: 이미 좋아요함 (409)

---

## 4. 브라우저 콘솔 체크

### 정상 동작 시
```
✅ No CORS errors
✅ No 401/403 errors (정당한 경우 제외)
✅ API 응답 구조: { message, data, timestamp }
✅ localStorage에 access_token, refresh_token 저장
```

### 주의 사항
```
⚠️ JWT 토큰 갱신 로직 확인 (401 → refresh → retry)
⚠️ 댓글 카운트 정확성 (off-by-one 수정 확인)
⚠️ XSS 방지 (escapeHtml 적용 확인)
⚠️ 이미지 TTL 처리 (업로드 후 1시간, 게시글 연결 시 영구 보존)
```

---

## 5. 성능 체크

### 네트워크 탭
- ✅ N+1 쿼리 없음 (batch-fetch-size: 100)
- ✅ 이미지 로딩 최적화
- ✅ Cursor 페이지네이션 (무한 스크롤)

### 응답 시간
- 게시글 목록: < 500ms
- 게시글 상세: < 300ms
- 댓글 목록: < 200ms
- 이미지 업로드: < 2s (5MB 기준)

---

## 6. 문제 해결

### 백엔드 서버가 시작되지 않을 때
1. 환경 변수 확인 (`echo $DB_URL`, `echo $JWT_SECRET`)
2. MySQL 실행 확인 (`mysql -u root -p`)
3. 포트 충돌 확인 (`lsof -i :8080`)

### 정적 리소스가 로드되지 않을 때
1. SecurityConfig 정적 리소스 허용 확인
2. 파일 경로 확인 (`/css/`, `/js/` prefix)
3. 브라우저 캐시 삭제 (Cmd+Shift+R)

### API 호출 실패 시
1. 브라우저 콘솔 확인 (CORS, 401, 403, 404)
2. 백엔드 로그 확인 (`show-sql: true`)
3. JWT 토큰 확인 (`localStorage.getItem('access_token')`)
4. API_BASE_URL 확인 (`http://localhost:8080`)

---

## 7. 변경 이력

| 날짜 | 버전 | 변경 내용 |
|------|------|-----------|
| 2025-10-16 | 1.0 | 초기 연동 테스트 가이드 작성 |
| 2025-10-17 | 1.1 | 환경 변수 방법 통일 (dotenv 사용), DB 관리 주체 명시 |
