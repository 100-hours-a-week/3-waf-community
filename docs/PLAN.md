# PLAN.md - KTB Community 프로젝트 구현 계획

## 프로젝트 개요

**프로젝트명**: KTB Community Platform  
**기술 스택**: Spring Boot 3.5.6, Java 24, MySQL 8.0+, JPA/Hibernate  
**아키텍처**: 3-Layer (Controller-Service-Repository)

---

## 현재 진행 상황

**Phase 1 완료** ✅
**Phase 2 완료** ✅ (Week 2-3)
Progress: ██████████ 100%

---

## 전체 로드맵

| Phase | Week | 목표 | FR 범위 | 상태 |
|-------|------|------|---------|------|
| Phase 1 | 1 | 기반 설정 | - | ✅ 완료 |
| Phase 2 | 2-3 | 인증/사용자 | AUTH-001~004, USER-001~004 | ✅ 완료 |
| Phase 3 | 4-5 | 게시글/댓글 | POST-001~005, COMMENT-001~004, LIKE-001~003 | ⏳ 대기 |
| Phase 4 | 6 | 이미지/통계 | IMAGE-001~003 | ⏳ 대기 |
| Phase 5 | 7 | 테스트/문서 | - | ⏳ 대기 |

---

## Phase 1: 프로젝트 기반 설정 ✅ 완료

**목표**: 개발 환경 구축 및 데이터베이스 스키마 구축

**완료 항목:**
- [x] Spring Boot 프로젝트 생성 (Java 24, Gradle)
- [x] MySQL 데이터베이스 설정
- [x] JPA Entity 클래스 8개 (User, Post, Comment, PostLike, Image, UserToken, PostStats, PostImage)
- [x] Enum 클래스 4개 (UserRole, UserStatus, PostStatus, CommentStatus)
- [x] 패키지 구조 설계

**참조**: **@docs/LLD.md Section 3** (패키지 구조), **@docs/DDL.md** (스키마)

---

## Phase 2: 인증 및 사용자 관리 ✅ 완료

**목표**: JWT 기반 인증 시스템 및 사용자 CRUD 구현

### FR 매핑

| FR 코드 | 기능 | 구현 위치 |
|---------|------|-----------|
| FR-AUTH-001 | 회원가입 | AuthService.signup() |
| FR-AUTH-002 | 로그인 | AuthService.login() |
| FR-AUTH-003 | 로그아웃 | AuthService.logout() |
| FR-AUTH-004 | 토큰 갱신 | AuthService.refreshToken() |
| FR-USER-001 | 사용자 조회 | UserService.getProfile() |
| FR-USER-002 | 사용자 수정 | UserService.updateProfile() |
| FR-USER-003 | 비밀번호 변경 | UserService.changePassword() |
| FR-USER-004 | 회원 탈퇴 | UserService.deactivateAccount() |

### 체크리스트

**인증 시스템:**
- [x] JwtTokenProvider (토큰 생성/검증)
- [x] UserToken 엔티티 (RDB 토큰 관리)
- [x] Spring Security 설정 (필터 체인)
- [x] BCryptPasswordEncoder (비밀번호 암호화)

**API 구현:**
- [x] 인증 API 3개 (POST /auth/login, /auth/logout, /auth/refresh_token)
- [x] 사용자 API 5개 (POST /users/signup, GET/PATCH /users/{id}, /users/{id}/password 등)
- 상세 스펙: **@docs/API.md Section 1-2**

**비즈니스 로직:**
- [x] 비밀번호 정책 검증 (8-20자, 대/소/특수문자)
- [x] 이메일/닉네임 중복 확인
- [x] Rate Limiting (분당 100회, Bucket4j 기반)

**동시성 제어:**
- [x] PostStatsRepository 원자적 UPDATE 메서드
  - incrementViewCount(), incrementLikeCount(), decrementLikeCount()
  - incrementCommentCount(), decrementCommentCount()
- [ ] Service 계층 통합 (Phase 3에서 PostService, CommentService, LikeService 구현 시)
- 상세: **@docs/LLD.md Section 7.2, 12.3**

**테스트:**
- [x] 단위 테스트 작성 (AuthService, UserService)
  - AuthServiceTest: 10/10 통과
  - UserServiceTest: 8/8 통과
  - JwtTokenProviderTest: 10/10 통과
- [x] Service Layer 커버리지 100% (18/18 통과)
- [ ] Repository Layer 테스트 (Phase 3에서 H2 설정과 함께 진행)

### 완료 조건
- [x] 회원가입 → 로그인 → 토큰 발급 → 인증 API 호출 플로우 작동
- [x] 비밀번호 정책 검증 통과
- [x] 모든 단위 테스트 통과 (Service Layer 18/18)

**참조**: **@docs/LLD.md Section 6 (인증)**, **Section 7.2 (좋아요 동시성), Section 12.3 (동시성 제어)**

---

## Phase 3: 게시글 및 댓글 기능

**목표**: 커뮤니티 핵심 기능 구현

### FR 매핑

| FR 코드              | 기능       | 구현 위치 |
|--------------------|----------|-----------|
| FR-POST-001~005    | 게시글 CRUD | PostService |
| FR-COMMENT-001~004 | 댓글 CRUD  | CommentService |
| FR-LIKE-001~003    | 좋아요      | LikeService |

### 체크리스트

**게시글 기능:**
- [ ] PostService (CRUD, 페이지네이션, 정렬)
- [ ] 권한 검증 (작성자만 수정/삭제)
- [ ] 조회수 자동 증가 (PostStats)
- [ ] API 6개 (POST/GET/PATCH/DELETE /posts)

**댓글 기능:**
- [ ] CommentService (CRUD)
- [ ] 권한 검증 (작성자만 수정/삭제)
- [ ] 댓글 수 자동 업데이트 (PostStats)
- [ ] API 4개 (GET/POST/PATCH/DELETE /posts/{id}/comments)

**좋아요 기능:**
- [ ] LikeService (추가/취소)
- [ ] 중복 방지 (user_id, post_id UNIQUE)
- [ ] 좋아요 수 자동 업데이트 (PostStats)
- [ ] API 3개 (POST/DELETE /posts/{id}/like, GET /users/me/likes)

**테스트:**
- [ ] 단위 테스트 (Service Layer 80%+)
- [ ] N+1 문제 검증 (Fetch Join)
- [ ] 동시성 테스트 (좋아요 동시 추가)

### 완료 조건
- 게시글/댓글/좋아요 전체 플로우 작동
- 권한 검증 정상 작동
- 모든 단위 테스트 통과

**참조**: **@docs/LLD.md Section 7 (비즈니스 로직), Section 12 (성능 최적화)**

---

## Phase 4: 이미지 처리 및 통계

**목표**: 이미지 관리 및 게시글 통계 기능 구현

### 체크리스트

**이미지 관리:**
- [ ] Image 엔티티 JPA 매핑
- [ ] PostImage 브릿지 테이블 (display_order 관리)
- [ ] 프로필 이미지 처리 (User-Image)
- [ ] S3 서비스 인터페이스 준비 (현재는 URL만 저장)

**통계 기능:**
- [ ] PostStats 자동 업데이트 (좋아요/댓글/조회수)
- [ ] 통계 기반 정렬 (인기순)

**테스트:**
- [ ] 단위 테스트 (Service Layer 80%+)
- [ ] 동시성 처리 검증

### 완료 조건
- 이미지 URL 저장 및 조회 작동
- 통계 자동 업데이트 확인
- S3 연동 인터페이스 준비

**참조**: **@docs/DDL.md (Image, PostImage 테이블)**

---

## Phase 5: 테스트 및 문서화

**목표**: 품질 확보 및 문서 정리

### 체크리스트

**테스트:**
- [ ] 전체 Service Layer 테스트 (커버리지 80%+)
- [ ] Repository Layer 테스트 (커버리지 60%+)
- [ ] 통합 테스트 주요 플로우

**문서화:**
- [ ] @docs/API.md 최종 검토
- [ ] Postman Collection 작성
- [ ] README 업데이트

**코드 품질:**
- [ ] 코드 리뷰 및 리팩토링
- [ ] 네이밍 컨벤션 통일
- [ ] 불필요한 주석 제거

### 완료 조건
- 전체 테스트 커버리지 60% 이상
- API 문서 최신화
- 코드 리뷰 완료

---

## 개발 규칙

**커밋 메시지 형식:**
```
feat: FR-POST-001 게시글 작성 API 구현
test: FR-AUTH-001 회원가입 단위 테스트 추가
fix: FR-LIKE-001 중복 좋아요 방지 버그 수정
docs: @docs/API.md 게시글 섹션 업데이트
```

**Phase 완료 기준:**
1. 모든 체크리스트 완료
2. 단위 테스트 통과 (Service 80%+)
3. 완료 조건 검증
4. PLAN.md 체크박스 업데이트

**문서 동기화:**
- 설계 변경 시 → @docs/LLD.md 업데이트
- API 변경 시 → @docs/API.md 업데이트
- 스키마 변경 시 → @docsDDL.md 업데이트

---

## 제약사항 (설계 배경)

**기술 제약:**
- 토큰: RDB 저장 (user_tokens) → 추후 Redis 전환
- 이미지: URL만 저장 → 추후 S3 연동

**성능 가정:**
- 초기 트래픽 낮음 → 단일 서버 충분
- 동시성 제어: 원자적 UPDATE (PostStats)

**데이터 정책:**
- Soft Delete: User, Post, Comment (status 변경)
- Hard Delete: UserToken (배치 작업)

상세: @docs/PRD.md Section 5, @docs/LLD.md Section 13

---

## Phase 6+ 확장 계획

**고도화:**
- [ ] Admin 관리 기능
- [ ] 게시글 검색 (제목/내용/작성자)
- [ ] 알림 기능

**인프라:**
- [ ] Redis 도입 (토큰/캐싱)
- [ ] S3 이미지 업로드
- [ ] Docker 컨테이너화
- [ ] CI/CD 파이프라인

---

## 주요 리스크

| 리스크 | 대응 방안 |
|--------|-----------|
| JWT RDB 성능 저하 | 인덱스 최적화, Redis 전환 |
| 동시성 이슈 | 원자적 UPDATE, 락 전략 |
| 이미지 처리 | S3 도입 전까지 URL만 |

---

## 참고 문서

- **요구사항**: @docs/PRD.md (FR/NFR 코드)
- **설계**: @docs/LLD.md (아키텍처, 패턴)
- **스키마**: @docs/DDL.md
- **API**: @docs/API.md
- **가이드**: @docs/CLAUDE.md