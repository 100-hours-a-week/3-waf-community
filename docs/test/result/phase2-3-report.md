# Phase 2-3 테스트 결과 리포트

## 테스트 정보
- **실행 일시**: 2025-10-17 15:06
- **테스트 대상**: 게시글 작성 및 상세 조회
- **테스트 도구**: MCP Chrome DevTools

## 테스트 결과: ⚠️ 부분 성공 (백엔드 버그 발견)

### Phase 2: 게시글 작성 ✅ 성공

#### 1. 게시글 작성 폼
- ✅ /board/write.html 이동 성공
- ✅ 제목 입력: "Phase 2 테스트 게시글"
- ✅ 내용 입력: "프론트엔드-백엔드 연동 테스트를 위한 첫 번째 게시글입니다..."
- ✅ 이미지 업로드: 스킵 (선택 사항)

#### 2. 게시글 생성 API
- ✅ POST /posts 성공 (추정)
- ✅ DB 저장 확인 (posts 테이블)
  - post_id: 7
  - post_title: "Phase 2 테스트 게시글"
  - post_content: "프론트엔드-백엔드 연동 테스트를 위한 첫 번째 게시글입니다. JWT 인증이 정상 작동하고 있으며, 게시글 작성 API를 테스트합니다."
  - post_status: ACTIVE
  - user_id: 11
  - created_at: 2025-10-17 13:08:55

#### 3. post_stats 테이블 확인
- ✅ post_id: 7
- ✅ like_count: 0
- ✅ comment_count: 0
- ✅ view_count: 0
- ✅ last_updated: 2025-10-17 13:08:55

#### 4. 게시글 목록 렌더링
- ✅ 목록 페이지로 리다이렉트
- ✅ 게시글 카드 표시 (제목, 작성자, 날짜, 통계)
- ✅ 작성자: "테스터"
- ✅ 상대 시간: "방금 전" → "1시간 전"
- ✅ 통계: 0/0/0 (좋아요/댓글/조회수)

---

### Phase 3: 게시글 상세 조회 ❌ 실패 (백엔드 버그)

#### 문제: GET /posts/7 실패
- ❌ GET /posts/7 → 400 Bad Request
- ❌ 에러 코드: COMMON-001
- ❌ 에러 메시지: "Entity not managed"

#### 백엔드 응답 (curl 테스트)
```json
{
  "message": "COMMON-001",
  "data": {
    "field": null,
    "details": "Entity not managed"
  },
  "timestamp": "2025-10-17T15:06:41.694711"
}
```

#### 프론트엔드 동작
- ❌ `loadPost()` 함수 catch 블록 실행
- ❌ alert("게시글을 찾을 수 없습니다.") 표시
- ❌ window.location.href = '/board/list.html' 리다이렉트

#### 백엔드 로그
```
2025-10-17 13:09:07 [http-nio-8080-exec-5] WARN  c.k.c.e.GlobalExceptionHandler - [Error] 잘못된 인자: Entity not managed
```

---

## 버그 분석

### 근본 원인: JPA Persistence Context 문제

**증상:**
1. 게시글이 데이터베이스에 존재함 (post_id=7)
2. 게시글 목록 조회 시 정상 표시됨 (GET /posts?limit=10&sort=latest)
3. 게시글 상세 조회 시 400 에러 발생 (GET /posts/7)

**가능한 원인:**
1. **Detached Entity**: 게시글 생성 후 Entity가 영속성 컨텍스트에서 분리됨
2. **Transaction Boundary**: 트랜잭션 범위 설정 오류
3. **Lazy Loading**: 연관 관계 로딩 실패 (user, images 등)
4. **Entity Manager**: flush() 또는 merge() 누락

**백엔드 수정 필요 파일:**
- `PostService.java`: getPostById() 메서드 확인
- `PostRepository.java`: findById() 쿼리 확인
- `Post.java`: Entity 매핑 확인 (FetchType, CascadeType)

---

## 관련 스크린샷

1. `test-phase2-write-form-filled.png`: 게시글 작성 폼
2. `test-phase2-post-created-in-list.png`: 게시글 목록에 표시된 새 게시글

---

## 다음 단계

### 1. 백엔드 버그 수정 필요
백엔드 개발자에게 전달할 정보:
- GET /posts/{id} 엔드포인트가 400 에러 반환
- 에러: "Entity not managed"
- 게시글은 DB에 존재하며 목록 조회는 정상 작동
- JPA Entity 관리 로직 점검 필요

### 2. 수정 후 재테스트
- Phase 3: 게시글 상세 조회
- Phase 4: 좋아요 및 댓글
- Phase 5: 프로필 관리

---

## 결론

**Phase 2 완료**: 게시글 작성 기능은 정상 작동합니다.
- POST /posts API 성공
- DB 저장 성공
- 목록 렌더링 성공

**Phase 3 블로킹**: 백엔드 버그로 인해 상세 조회 실패
- GET /posts/{id} 400 에러
- "Entity not managed" JPA 이슈
- 백엔드 수정 필요

**테스트 진행률**: Phase 1 완료, Phase 2 완료, Phase 3-5 블로킹
