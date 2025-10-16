# PLAN.md - KTB Community 프로젝트 구현 계획

## 프로젝트 개요

**프로젝트명**: KTB Community Platform  
**기술 스택**: Spring Boot 3.5.6, Java 24, MySQL 8.0+, JPA/Hibernate  
**아키텍처**: 3-Layer (Controller-Service-Repository)

---

## 현재 진행 상황

**Phase 1 완료** ✅
**Phase 2 완료** ✅ (Week 2-3)
**Phase 3 완료** ✅ (Week 4-5)
**Phase 3.5 완료** ✅ (S3 이미지 업로드)
**Phase 3.6 완료** ✅ (회원가입/프로필 Multipart 전환 + P0/P1 수정)

---

## 전체 로드맵

| Phase | Week | 목표 | FR 범위 | 상태 |
|-------|------|------|---------|------|
| Phase 1 | 1 | 기반 설정 | - | ✅ 완료 |
| Phase 2 | 2-3 | 인증/사용자 | AUTH-001~004, USER-001~004 | ✅ 완료 |
| Phase 3 | 4-5 | 게시글/댓글/좋아요 | POST-001~005, COMMENT-001~004, LIKE-001~003 | ✅ 완료 |
| Phase 3.5 | 5 | 이미지 업로드 (S3) | IMAGE-001, IMAGE-003 | ✅ 완료 |
| Phase 3.6 | 5 | Multipart 전환 + P0/P1 수정 | AUTH-001, USER-002 | ✅ 완료 |
| Phase 4 | 6 | 통계 및 배치 | IMAGE-002 (고아 이미지) | ✅ 완료 |
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
- [x] 단위 테스트 작성 (AuthService, UserService, JwtTokenProvider, RateLimitAspect)
  - AuthServiceTest: 8/8 통과
  - UserServiceTest: 7/7 통과
  - JwtTokenProviderTest: 9/9 통과
  - RateLimitAspectTest: 10/10 통과
- [x] Service Layer 커버리지 100% (15/15 통과)
- [x] Security Layer 테스트 (JwtTokenProvider 9/9 통과)
- [x] Config Layer 테스트 (RateLimitAspect 10/10 통과)
- [x] Repository Layer 테스트 (Phase 3에서 완료, H2 환경 12/12 통과)

### 완료 조건
- [x] 회원가입 → 로그인 → 토큰 발급 → 인증 API 호출 플로우 작동
- [x] 비밀번호 정책 검증 통과
- [x] 모든 단위 테스트 통과 (Service Layer 15/15, Security Layer 9/9, Config Layer 10/10)

**참조**: **@docs/LLD.md Section 6 (인증)**, **Section 7.2 (좋아요 동시성), Section 12.3 (동시성 제어)**

---

## Phase 3: 게시글 및 댓글 기능 ✅ 완료

**목표**: 커뮤니티 핵심 기능 구현

### FR 매핑

| FR 코드              | 기능       | 구현 위치 |
|--------------------|----------|-----------|
| FR-POST-001~005    | 게시글 CRUD | PostService |
| FR-COMMENT-001~004 | 댓글 CRUD  | CommentService |
| FR-LIKE-001~003    | 좋아요      | LikeService |

### 체크리스트

**게시글 기능:**
- [x] PostService (CRUD, 페이지네이션, 정렬)
- [x] 권한 검증 (작성자만 수정/삭제)
- [x] 조회수 자동 증가 (PostStats, EntityManager.refresh 동기화)
- [x] API 6개 (POST/GET/PATCH/DELETE /posts)
- [x] PostController 구현

**댓글 기능:**
- [x] CommentService (CRUD)
- [x] 권한 검증 (작성자만 수정/삭제)
- [x] 댓글 수 자동 업데이트 (PostStats 원자적 UPDATE)
- [x] API 4개 (GET/POST/PATCH/DELETE /posts/{id}/comments)
- [x] CommentController 구현

**좋아요 기능:**
- [x] LikeService (추가/취소)
- [x] 중복 방지 (user_id, post_id UNIQUE)
- [x] 좋아요 수 자동 업데이트 (PostStats 원자적 UPDATE)
- [x] API 3개 (POST/DELETE /posts/{id}/like, GET /users/me/likes)

**Repository 계층:**
- [x] PostRepository (Fetch Join N+1 방지, JPQL 수정)
- [x] CommentRepository (Fetch Join, JPQL 수정)
- [x] PostLikeRepository (좋아요 목록, JPQL 수정)
- [x] ImageRepository 생성 (프로필 이미지 연동)

**테스트:**
- [x] 단위 테스트 (Service Layer 30개, 100% 통과)
  - PostServiceTest: 11/11 통과
  - CommentServiceTest: 10/10 통과
  - LikeServiceTest: 9/9 통과
- [x] Repository 테스트 (12/12 통과, H2 환경)
- [x] N+1 문제 검증 (Fetch Join)
- [x] 전체 테스트: 98/98 통과 (100%)

### 완료 조건
- [x] 게시글/댓글/좋아요 전체 플로우 작동
- [x] 권한 검증 정상 작동
- [x] 모든 단위 테스트 통과

**참조**: **@docs/LLD.md Section 7 (비즈니스 로직), Section 12 (성능 최적화)**

---

## Phase 3.5: 이미지 업로드 인프라 ✅ 완료

**목표**: S3 직접 연동 이미지 업로드 시스템 구현

### FR 매핑

| FR 코드 | 기능 | 구현 위치 |
|---------|------|-----------|
| FR-IMAGE-001 | 이미지 정보 저장 | ImageRepository |
| FR-IMAGE-003 | 이미지 업로드 | ImageService |

### 체크리스트

**이미지 업로드:**
- [x] ImageService (파일 검증, S3 업로드, DB 저장)
- [x] ImageController (POST /images)
- [x] S3Client 설정 (AWS SDK v2)
- [x] 파일 검증 (크기, 형식, Magic Number)
- [x] expires_at TTL 로직 (1시간)

**통합:**
- [x] PostService 이미지 연결 (clearExpiresAt)
- [x] UserService 프로필 이미지 연결
- [x] PostImage 브릿지 테이블 처리

**테스트:**
- [x] ImageService 단위 테스트
- [x] 파일 검증 로직 테스트
- [x] S3 업로드 통합 테스트

### 완료 조건
- [x] POST /images API 작동 (multipart/form-data)
- [x] S3 업로드 및 DB 저장 확인
- [x] 게시글/프로필 이미지 연결 작동
- [x] 모든 단위 테스트 통과

**참조**: **@docs/LLD.md Section 7.5** (이미지 업로드 흐름), **@docs/API.md Section 4.1**

---

## Phase 3.6: 회원가입/프로필 Multipart 전환 ✅ 완료

**목표**: 회원가입과 프로필 수정 시 이미지와 데이터를 함께 전송하는 자연스러운 UX 구현

### FR 매핑

| FR 코드 | 기능 | 변경 내용 |
|---------|------|-----------|
| FR-AUTH-001 | 회원가입 | 2단계 → Multipart 직접 업로드 |
| FR-USER-002 | 프로필 수정 | 2단계 → Multipart 직접 업로드 |

### 체크리스트

**문서 업데이트:**
- [x] PLAN.md (Phase 3.5 완료, Phase 3.6 추가)
- [x] PRD.md (FR-AUTH-001, FR-USER-002)
- [x] API.md (Section 2.1, 2.3)
- [x] LLD.md (Section 7.5 - 2가지 업로드 패턴)

**DTO 수정:**
- [x] SignupRequest - profileImageId 제거
- [x] UpdateProfileRequest - profileImageId 제거

**Controller 수정:**
- [x] UserController.signup() - Multipart 적용 (@RequestPart)
- [x] UserController.updateProfile() - Multipart 적용 (@RequestPart)

**Service 수정:**
- [x] AuthService.signup() - ImageService 통합 (MultipartFile 파라미터)
- [x] UserService.updateProfile() - ImageService 통합 (MultipartFile 파라미터)

**테스트 수정:**
- [x] AuthServiceTest - MultipartFile null 처리
- [x] UserServiceTest - MultipartFile null 처리
- [x] UserControllerIntegrationTest - Manual Validation 검증 (P0/P1)

**P0/P1 수정:**
- [x] P0: @RequestPart Manual Validation 복원 (40자 닉네임 → 400 에러)
- [x] P1: PasswordValidator 사용으로 ErrorCode 일관성 복원 (USER-004)

### 완료 조건
- [x] Multipart 회원가입/프로필 수정 작동
- [x] Manual Validation으로 입력 검증 (Bean Validation 대체)
- [x] 모든 단위 테스트 통과 (102 tests, 0 failures, 100% success)

**참조**: **@docs/LLD.md Section 7.5** (2가지 업로드 패턴), **@docs/API.md Section 2.1, 2.3**

---

## Phase 4: 통계 및 배치 작업

**목표**: 게시글 통계 활용 및 고아 이미지 정리 배치 구현

### 체크리스트

**통계 기능:**
- [x] PostStats 자동 업데이트 검증 (Phase 3에서 구현됨)
- [x] 통계 기반 정렬 구현 (인기순: like_count DESC)
- [x] 통계 조회 최적화 (N+1 방지)

**고아 이미지 배치:**
- [x] 배치 작업 스케줄러 (@Scheduled)
- [x] expires_at < NOW() 조건 이미지 조회
- [x] S3 파일 삭제
- [x] DB 레코드 삭제
- [x] 배치 로그 기록

**테스트:**
- [x] 통계 정렬 테스트 (Phase 3에서 완료)
- [x] 배치 작업 단위 테스트
- [x] TTL 만료 시나리오 검증

### 완료 조건
- 고아 이미지 배치 작업 스케줄 실행
- 배치 로그 확인

**참조**: **@docs/LLD.md Section 7.5** (고아 이미지 처리)

---

## Phase 5: 테스트 및 문서화

**목표**: 품질 확보 및 문서 정리

### 체크리스트

**페이지네이션:**
- [x] Cursor 페이지네이션 전환 (최신순만, 하이브리드 방식)
  - Repository: findByStatusWithCursor, findByStatusWithoutCursor 추가
  - Service: getPosts 시그니처 변경 (cursor, offset 파라미터)
  - Controller: cursor/offset 파라미터 추가
  - 응답 구조: latest (cursor/hasMore), likes (offset/total_count)
- [ ] Cursor 페이지네이션 확장 (likes 정렬, 추후 작업)
- [ ] GET /posts/users/me/likes cursor 전환 (추후 작업)

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
- 스키마 변경 시 → @docs/DDL.md 업데이트

---

## 제약사항 (설계 배경)

**기술 제약:**
- 토큰: RDB 저장 (user_tokens) → 추후 Redis 전환
- 이미지: S3 직접 저장 (Phase 3.5부터)

**성능 가정:**
- 초기 트래픽 낮음 → 단일 서버 충분
- 동시성 제어: 원자적 UPDATE (PostStats)

**데이터 정책:**
- Soft Delete: User, Post, Comment (status 변경)
- Hard Delete: UserToken (배치 작업), 만료된 Image (Phase 4)

상세: @docs/PRD.md Section 5, @docs/LLD.md Section 7.5

---

## Phase 6+ 확장 계획

**고도화:**
- [ ] Admin 관리 기능
- [ ] 게시글 검색 (제목/내용/작성자)
- [ ] 알림 기능

**인프라:**
- [ ] Redis 도입 (토큰/캐싱)
- [ ] Docker 컨테이너화
- [ ] CI/CD 파이프라인

---

## 주요 리스크

| 리스크 | 대응 방안 |
|--------|-----------|
| JWT RDB 성능 저하 | 인덱스 최적화, Redis 전환 |
| 동시성 이슈 | 원자적 UPDATE, 락 전략 |
| 고아 이미지 누적 | TTL 기반 배치 삭제 (Phase 4) |
| S3 비용 초과 | Free Tier 모니터링, 압축 최적화 |

---

## 참고 문서

- **요구사항**: @docs/PRD.md (FR/NFR 코드)
- **설계**: @docs/LLD.md (아키텍처, 패턴)
- **스키마**: @docs/DDL.md
- **API**: @docs/API.md
- **가이드**: @CLAUDE.md