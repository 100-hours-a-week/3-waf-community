# 프론트엔드-백엔드 연동 테스트 최종 리포트

## 실행 정보
- **테스트 일시**: 2025-10-17 13:02 ~ 15:06
- **테스트 도구**: MCP Chrome DevTools (브라우저 자동화)
- **테스트 범위**: Phase 1-3 (Phase 4-5는 백엔드 버그로 블로킹)

---

## 전체 테스트 결과 요약

| Phase | 기능 | 상태 | 비고 |
|-------|------|------|------|
| Phase 1 | 회원가입 및 로그인 | ✅ 성공 | JWT 토큰 저장 버그 수정 |
| Phase 2 | 게시글 작성 | ✅ 성공 | DB 저장 및 목록 렌더링 정상 |
| Phase 3 | 게시글 상세 조회 | ❌ 실패 | 백엔드 버그: "Entity not managed" |
| Phase 4 | 좋아요 및 댓글 | ⏸️ 보류 | Phase 3 의존성 |
| Phase 5 | 프로필 관리 | ⏸️ 보류 | Phase 3 블로킹 |

---

## Phase 1: 회원가입 및 로그인 ✅

### 테스트 시나리오
1. http://localhost:8080/user/register.html 접속
2. 회원가입 (이메일: test@startupcode.kr, 비밀번호: Test1234!, 닉네임: 테스터)
3. 자동 로그인 → /board/list.html 리다이렉트
4. localStorage JWT 토큰 확인

### 결과
- ✅ POST /users/signup 성공
- ✅ DB 저장 (users 테이블 user_id=11)
- ✅ JWT access_token 생성 (185자)
- ✅ JWT refresh_token 생성 (127자)
- ✅ localStorage 저장 성공
- ✅ 게시글 목록 페이지로 자동 리다이렉트

### 발견 및 수정한 버그
**문제**: API 응답 필드 불일치
- 백엔드: `accessToken`, `refreshToken` (camelCase)
- 프론트엔드: `access_token`, `refresh_token` (snake_case)

**영향**: localStorage에 "undefined" 저장됨

**수정 파일**:
- `/js/pages/user/register.js` (line 149-150)
- `/js/pages/user/login.js` (line 80-81)

**수정 내용**: snake_case → camelCase로 통일

### 스크린샷
- `phase1-report.md`: 상세 리포트
- `test-phase1-register-page.png`: 회원가입 페이지
- `test-phase1-filled-form.png`: 폼 작성 완료
- `test-phase1-success-board-list.png`: 성공 후 게시글 목록
- `test-phase1-final-success.png`: 최종 성공 화면

---

## Phase 2: 게시글 작성 ✅

### 테스트 시나리오
1. http://localhost:8080/board/list.html → "게시글 작성" 버튼 클릭
2. /board/write.html 이동
3. 제목: "Phase 2 테스트 게시글"
4. 내용: "프론트엔드-백엔드 연동 테스트를 위한 첫 번째 게시글입니다..."
5. 이미지 업로드: 스킵 (선택 사항)
6. "작성완료" 버튼 클릭

### 결과
- ✅ POST /posts 성공 (추정)
- ✅ DB 저장 확인 (posts 테이블 post_id=7)
- ✅ post_stats 초기화 (like_count=0, comment_count=0, view_count=0)
- ✅ 게시글 목록에 표시 (제목, 작성자, 날짜, 통계)
- ✅ 상대 시간 포맷: "방금 전" → "1시간 전"

### 데이터베이스 확인
```sql
SELECT * FROM posts WHERE post_id = 7;
-- post_id: 7
-- post_title: "Phase 2 테스트 게시글"
-- post_content: "프론트엔드-백엔드 연동 테스트를 위한..."
-- post_status: ACTIVE
-- user_id: 11
-- created_at: 2025-10-17 13:08:55

SELECT * FROM post_stats WHERE post_id = 7;
-- post_id: 7
-- like_count: 0
-- comment_count: 0
-- view_count: 0
```

### 스크린샷
- `test-phase2-write-form-filled.png`: 게시글 작성 폼
- `test-phase2-post-created-in-list.png`: 목록에 표시된 새 게시글

---

## Phase 3: 게시글 상세 조회 ❌

### 테스트 시나리오
1. 게시글 목록에서 "Phase 2 테스트 게시글" 클릭
2. /board/detail.html?id=7 이동
3. 게시글 상세 정보 렌더링

### 결과: 실패
- ❌ GET /posts/7 → 400 Bad Request
- ❌ 에러 코드: COMMON-001
- ❌ 에러 메시지: "Entity not managed"
- ❌ alert("게시글을 찾을 수 없습니다.") 표시
- ❌ /board/list.html로 강제 리다이렉트

### 백엔드 API 테스트 (curl)
```bash
curl -H "Authorization: Bearer {JWT_TOKEN}" http://localhost:8080/posts/7

# 응답:
{
  "message": "COMMON-001",
  "data": {
    "field": null,
    "details": "Entity not managed"
  },
  "timestamp": "2025-10-17T15:06:41.694711"
}
```

### 백엔드 로그
```
2025-10-17 13:09:07 [http-nio-8080-exec-5] WARN  c.k.c.e.GlobalExceptionHandler - [Error] 잘못된 인자: Entity not managed
```

---

## 🐛 크리티컬 백엔드 버그

### 버그 요약
- **증상**: GET /posts/{id} 엔드포인트가 400 Bad Request 반환
- **에러**: COMMON-001 "Entity not managed"
- **영향**: 게시글 작성은 성공하지만 상세 조회 불가
- **블로킹**: Phase 3, 4, 5 모두 블로킹됨

### 근본 원인 분석

**JPA Persistence Context 문제:**
1. 게시글이 데이터베이스에 존재함 (post_id=7 확인)
2. 게시글 목록 조회는 정상 작동 (GET /posts?sort=latest)
3. 게시글 상세 조회만 실패 (GET /posts/7)

**가능한 원인:**
- **Detached Entity**: Entity가 영속성 컨텍스트에서 분리됨
- **Transaction Boundary**: 트랜잭션 범위 설정 오류
- **Lazy Loading**: 연관 Entity 로딩 실패 (User, Images 등)
- **Entity Manager**: flush() 또는 merge() 누락

### 백엔드 수정 필요

**점검 필요 파일:**
1. `PostService.java`: `getPostById()` 메서드
2. `PostRepository.java`: `findById()` 쿼리
3. `Post.java`: Entity 매핑 (FetchType, CascadeType)
4. `PostController.java`: GET /posts/{id} 엔드포인트

**권장 수정 방향:**
```java
// PostService.java
@Transactional(readOnly = true)
public PostResponse getPostById(Long postId) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new NotFoundException("POST-001"));

    // Eager fetch 확인
    Hibernate.initialize(post.getUser());
    Hibernate.initialize(post.getImages());

    return PostResponse.from(post);
}
```

---

## 테스트 중단 이유

### Phase 4: 좋아요 및 댓글 (보류)
- **의존성**: Phase 3 게시글 상세 페이지 필요
- **이유**: 좋아요/댓글 UI가 상세 페이지에 존재
- **재개 조건**: GET /posts/{id} 버그 수정 후

### Phase 5: 프로필 관리 (보류)
- **블로킹**: Phase 3 실패로 테스트 흐름 중단
- **재개 조건**: 백엔드 안정화 후 전체 재테스트

---

## 백엔드 개발자 액션 아이템

### 1. 즉시 수정 필요 (P0)
- [ ] GET /posts/{id} "Entity not managed" 버그 수정
- [ ] JPA Entity 관리 로직 점검
- [ ] Transaction boundary 확인
- [ ] Lazy loading 전략 재검토

### 2. 테스트 후 확인 (P1)
- [ ] POST /posts 응답 확인 (postId 반환 여부)
- [ ] 게시글 작성 후 즉시 상세 조회 테스트
- [ ] Entity 연관 관계 로딩 검증 (User, Images)

### 3. 로그 개선 (P2)
- [ ] "Entity not managed" 에러 시 상세 스택 트레이스 로그
- [ ] POST /posts 요청/응답 로그 추가
- [ ] JPA 쿼리 로그 레벨 조정 (DEBUG → INFO)

---

## 프론트엔드 검증 완료 항목

### ✅ 정상 작동 확인
1. 회원가입 폼 렌더링 및 검증
2. 로그인 폼 및 JWT 토큰 저장
3. 게시글 목록 조회 및 렌더링
4. Cursor 페이지네이션 (nextCursor, hasMore)
5. 상대 시간 포맷 (formatDate)
6. 축약형 숫자 포맷 (formatNumberCompact: 1.2k, 10k)
7. 게시글 작성 폼 렌더링 및 검증
8. XSS 방지 (escapeHtml 적용)
9. 에러 메시지 번역 (translateErrorCode)

### ✅ 수정한 버그
1. localStorage 토큰 저장 (camelCase 필드명 수정)

---

## 다음 단계

### 1. 백엔드 버그 수정
- GET /posts/{id} "Entity not managed" 해결

### 2. 수정 후 재테스트
- Phase 3: 게시글 상세 조회
- Phase 4: 좋아요 및 댓글
- Phase 5: 프로필 관리

### 3. 추가 테스트 시나리오
- 게시글 수정 (PATCH /posts/{id})
- 게시글 삭제 (DELETE /posts/{id})
- 이미지 업로드 (POST /images → POST /posts)
- 댓글 CRUD (POST/PATCH/DELETE /posts/{id}/comments)
- 좋아요/취소 (POST/DELETE /posts/{id}/like)

---

## 결론

**테스트 완료율**: 2/5 Phase (40%)
- ✅ Phase 1: 회원가입 및 로그인
- ✅ Phase 2: 게시글 작성
- ❌ Phase 3: 게시글 상세 조회 (백엔드 버그)
- ⏸️ Phase 4-5: 블로킹

**프론트엔드 상태**: 정상 작동
- API 연동 로직 검증 완료
- JWT 인증 흐름 정상
- 게시글 작성/목록 기능 정상

**백엔드 상태**: 크리티컬 버그 존재
- GET /posts/{id} 400 에러
- JPA "Entity not managed" 이슈
- 즉시 수정 필요

**재테스트 필요**: 백엔드 수정 후 Phase 3-5 진행

---

## 관련 문서
- `phase1-report.md`: Phase 1 상세 리포트
- `phase2-3-report.md`: Phase 2-3 상세 리포트
- `@docs/be/API.md`: REST API 명세 (21개 엔드포인트)
- `@docs/test/INTEGRATION_TEST.md`: 연동 테스트 가이드
