# LLD.md - Low Level Design Document

## 문서 정보

| 항목 | 내용                        |
|------|---------------------------|
| 프로젝트명 | KTB Community Platform    |
| 버전 | 1.4                       |
| 문서 유형 | Low Level Design Document |

---

## 1. 기술 스택

**백엔드:** Spring Boot 3.5.6, Java 24, Gradle 8.x  
**데이터베이스:** MySQL 8.0+, JPA (Hibernate), HikariCP  
**보안:** Spring Security, JWT, BCrypt  
**Storage:** AWS S3 (이미지 직접 저장, Free Tier)  
**추후:** Redis (토큰 캐싱)

**패키지 루트:** `com.ktb.community`

---

## 2. 시스템 아키텍처

### 2.1 전체 구조

```
Client (Frontend)
    ↓ HTTPS REST API
Controller Layer (요청/응답 처리)
    ↓
Service Layer (비즈니스 로직, @Transactional)
    ↓
Repository Layer (데이터 접근, JPA)
    ↓
MySQL Database
```

### 2.2 계층별 책임

**Controller:**
- DTO 검증: @Valid + Bean Validation (표준), Manual Validation (@RequestPart 예외)
- 요청 → Service 전달, 응답 포매팅
- 예외 메시지: 영어 통일 (ErrorCode 기본 메시지와 일관성)
- 위치: `com.ktb.community.controller`

**Service:**
- 비즈니스 로직, 트랜잭션 관리
- 엔티티 ↔ DTO 변환
- 위치: `com.ktb.community.service`

**Repository:**
- CRUD, 커스텀 쿼리 (JPA)
- 위치: `com.ktb.community.repository`

---

## 3. 패키지 구조

**주요 패키지:**
- `config/` - SecurityConfig, JwtConfig, WebConfig
- `controller/` - AuthController, UserController, PostController, CommentController, LikeController
- `service/` - AuthService, UserService, PostService, CommentService, LikeService, ImageService
- `repository/` - UserRepository, PostRepository, CommentRepository, PostLikeRepository, ImageRepository, UserTokenRepository, PostStatsRepository
- `entity/` - User, Post, Comment, PostLike, Image, UserToken, PostStats, PostImage
- `dto/request/` - LoginRequest, SignupRequest, PostCreateRequest, CommentCreateRequest
- `dto/response/` - ApiResponse, UserResponse, PostResponse, CommentResponse
- `security/` - JwtTokenProvider, JwtAuthenticationFilter, CustomUserDetailsService
- `exception/` - GlobalExceptionHandler, CustomException 계층
- `util/` - PasswordValidator, DateTimeUtil
- `enums/` - UserStatus, PostStatus, CommentStatus, UserRole

**상세 파일 구조:** 필요 시 IDE 탐색 또는 프로젝트 루트 참조

---

## 4. 데이터베이스 설계

**테이블 구조 및 DDL:** `@docs/DDL.md` 참조

**핵심 관계:**
- `users` 1:N → `posts`, `comments`, `post_likes`, `user_tokens`
- `posts` 1:1 → `post_stats`, 1:N → `comments`, M:N → `images` (via `post_images`)
- `post_likes` 복합 유니크 키: `(user_id, post_id)` 중복 방지

**주요 인덱스:**
- `users`: email, nickname (UNIQUE), user_status
- `posts`: created_at, (user_id, created_at)
- `comments`: (post_id, created_at, comment_id)
- `post_likes`: post_id, (user_id, post_id) UNIQUE

---

## 5. API 설계

**전체 API 스펙:** `@docs/API.md` 참조

### 공통 응답 구조
```json
{
  "message": "작업_결과_메시지",
  "data": { /* 응답 데이터 또는 null */ },
  "timestamp": "2025-10-01T14:30:00"
}
```

### 엔드포인트 분류
- **Auth:** /auth/login, /auth/logout, /auth/refresh_token
- **Users:** /users/signup, /users/{userID}, /users/{userID}/password
- **Posts:** /posts (목록, 작성), /posts/{postId} (상세, 수정, 삭제)
- **Comments:** /posts/{postId}/comments
- **Likes:** /posts/{postId}/like, /users/me/likes

---

## 6. 인증 및 보안

### 6.1 JWT 토큰

**Access Token:** 30분, API 인증  
**Refresh Token:** 7일, Access 갱신, `user_tokens` 테이블 저장

**Payload 예시:**
```json
{
  "sub": "user_id",
  "email": "user@example.com",
  "role": "USER",
  "iat": 1234567890,
  "exp": 1234569690
}
```

### 6.2 인증 흐름

**로그인:**  
Client → POST /auth/login → 서버 BCrypt 검증 → Access + Refresh 토큰 반환 → Refresh를 user_tokens에 저장

**API 호출:**  
Client → Authorization: Bearer {token} → JwtAuthenticationFilter 검증 → SecurityContext 저장 → 비즈니스 로직 실행

**토큰 갱신:**  
Client → POST /auth/refresh_token → user_tokens 테이블 검증 → 새 Access Token 반환

### 6.3 핵심 보안 설정

**SecurityConfig 핵심:**
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(session -> 
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/**", "/posts").permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthenticationFilter(), 
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

### 6.4 비밀번호 정책

**정책:**
- 길이: 8자 이상, 20자 이하
- 대문자: 최소 1개 (`[A-Z]`)
- 소문자: 최소 1개 (`[a-z]`)
- 특수문자: 최소 1개 (`[!@#$%^&*(),.?":{}|<>]`)

**구현:**
```java
public class PasswordValidator {
    private static final String REGEX =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?\":{}|<>]).{8,20}$";

    public static boolean isValid(String password) {
        return password != null && password.matches(REGEX);
    }
}
```

### 6.5 Rate Limiting

**정책:**
- 제한: 분당 100회
- 키: FQCN.methodName + IP 주소 + 사용자 ID (인증 시)
- 저장소: 인메모리 (Caffeine Cache), 추후 Redis
- 응답: 429 Too Many Requests

**설계 결정사항:**

| 항목 | 선택 | 이유 |
|------|------|------|
| 알고리즘 | Token Bucket | Burst traffic 허용, 점진적 refill, 산업 표준 |
| 라이브러리 | Bucket4j | Redis 전환 용이, 경량, Spring 친화적 |
| 캐시 | Caffeine | 자동 만료 (10분), 메모리 상한 (10k), LRU 지원 |
| 키 형식 | FQCN.methodName:IP:userId | 패키지 다른 동일 클래스명 충돌 방지 |
| 엔드포인트 격리 | FQCN 포함 | RESTful CRUD 메서드명 중복 대응 |

**구현 (AOP):**
```java
@Aspect
@Component
public class RateLimitAspect {
    // Caffeine Cache: 10분 미사용 시 자동 삭제, 최대 10,000개
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .build();

    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String clientKey = getClientKey(pjp);
        int requestsPerMinute = rateLimit.requestsPerMinute();

        // Token Bucket: 600ms마다 1개 토큰 refill
        Bucket bucket = buckets.get(clientKey, k -> createBucket(requestsPerMinute));

        if (!bucket.tryConsume(1)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
        return pjp.proceed();
    }

    // 키 예시: "com.ktb.community.controller.AuthController.login:192.168.1.1:user@example.com"
    private String getClientKey(ProceedingJoinPoint pjp) {
        String methodName = pjp.getSignature().getDeclaringTypeName()
                          + "." + pjp.getSignature().getName();
        // ... IP + userId 조합
        return methodName + ":" + userKey;
    }
}
```

**적용 대상:**
- AuthController.login - 분당 5회
- AuthController.signup - 분당 3회
- 기타 인증 API - 분당 100회 (기본값)

---

## 7. 주요 비즈니스 로직

### 7.1 게시글 작성 흐름

**핵심 구현 패턴:**
```java
@Transactional
public PostResponse createPost(PostCreateRequest request, Long userId) {
    // 1. 사용자 검증
    User user = userRepository.findById(userId)
            .orElseThrow(() -&gt; new BusinessException(ErrorCode.USER_NOT_FOUND,
                    "User not found with id: " + userId));

    // 2. 게시글 생성 및 저장
    Post post = request.toEntity(user);
    Post savedPost = postRepository.save(post);

    // 3. 통계 초기화 (Builder 기본값 0 사용)
    PostStats stats = PostStats.builder()
            .post(savedPost)
            .build();
    PostStats savedStats = postStatsRepository.save(stats);
    
    // 4. Post에 stats 연결 (필수 - PostResponse null 방지)
    savedPost.updateStats(savedStats);

    // 5. 이미지 TTL 해제 (imageId 있을 경우)
    if (request.getImageId() != null) {
        Image image = imageRepository.findById(request.getImageId())
                .orElseThrow(() -&gt; new BusinessException(ErrorCode.IMAGE_NOT_FOUND,
                        "Image not found with id: " + request.getImageId()));
        
        image.clearExpiresAt();  // expires_at → NULL (영구 보존)
        
        PostImage postImage = PostImage.builder()
                .post(savedPost)
                .image(image)
                .displayOrder(1)
                .build();
        postImageRepository.save(postImage);
    }

    return PostResponse.from(savedPost);
}
```

**설계 결정사항:**
- **예외**: BusinessException + ErrorCode 사용 (ResourceNotFoundException 아님)
- **통계 초기화**: Builder 기본값 의존 (명시적 0 설정 불필요)
- **updateStats() 필수**: 양방향 연관관계 동기화, PostResponse null 방지
- **이미지 TTL**: clearExpiresAt() 호출로 영구 보존 전환

**참조:** PostService.java:49-105

### 7.2 좋아요 처리 - 동시성 제어

**문제:** 동시 좋아요 시 Race Condition  
**해결:** DB 레벨 원자적 UPDATE

```java
@Modifying(clearAutomatically = true)
@Query("UPDATE PostStats ps SET ps.likeCount = ps.likeCount + 1, " +
       "ps.lastUpdated = CURRENT_TIMESTAMP WHERE ps.postId = :postId")
int incrementLikeCount(@Param("postId") Long postId);
```

**적용:**
- `incrementLikeCount()` / `decrementLikeCount()`
- `incrementCommentCount()` / `decrementCommentCount()`
- `incrementViewCount()`

**선택 이유:** 낙관적 락(재시도 폭증), 비관적 락(과도) 대비 최적

### 7.3 페이지네이션

**Offset/Limit (웹 환경):**
```java
Pageable pageable = PageRequest.of(offset / limit, limit, getSort(sort));
Page<Post> page = postRepository.findByStatus(PostStatus.ACTIVE, pageable);
```
- 장점: 페이지 번호 이동, Spring Data 지원
- 단점: Offset 클수록 성능 저하

**Cursor 기반 (모바일 무한 스크롤):**
```java
List<Post> posts = cursor == null 
    ? postRepository.findTopN(...) 
    : postRepository.findByIdLessThan(cursor, ...);
Long nextCursor = posts.isEmpty() ? null : posts.get(posts.size()-1).getId();
```
- 장점: 인덱스 활용, 실시간 안정성
- 단점: 특정 페이지 이동 불가

**권장:** 웹은 Offset/Limit, 모바일은 Cursor

### 7.4 댓글 작성 흐름

**핵심 구현 패턴:**
```java
@Transactional
public CommentResponse createComment(Long postId, CommentCreateRequest request, Long userId) {
    // 1. 게시글 존재 확인 (Fetch Join, ACTIVE만)
    Post post = postRepository.findByIdWithUserAndStats(postId, PostStatus.ACTIVE)
            .orElseThrow(() -&gt; new BusinessException(ErrorCode.POST_NOT_FOUND,
                    "Post not found with id: " + postId));

    // 2. 사용자 확인
    User user = userRepository.findById(userId)
            .orElseThrow(() -&gt; new BusinessException(ErrorCode.USER_NOT_FOUND,
                    "User not found with id: " + userId));

    // 3. 댓글 생성 및 저장
    Comment comment = request.toEntity(post, user);
    Comment savedComment = commentRepository.save(comment);

    // 4. 댓글 수 자동 증가 (동시성 제어 - 원자적 UPDATE)
    postStatsRepository.incrementCommentCount(postId);

    return CommentResponse.from(savedComment);
}
```

**설계 결정사항:**
- **Repository 메서드**: findByIdWithUserAndStats (Fetch Join + ACTIVE 필터링)
- **동시성 제어**: incrementCommentCount() - DB 레벨 원자적 UPDATE (Section 12.3)
- **트랜잭션 경계**: 댓글 저장 + 통계 증가가 동일 트랜잭션 (원자성 보장)

**권한 검증 패턴 (수정/삭제):**
```java
// 작성자 본인만 수정 가능
if (!comment.getUser().getUserId().equals(userId)) {
    throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS,
            "Not authorized to update this comment");
}
```

**참조**: CommentService.java (전체 CRUD), **@docs/API.md Section 5**, **@docs/DDL.md** (comments 테이블)

---

### 7.5 이미지 업로드 전략

**2가지 패턴 비교:**

| 항목 | Multipart 직접 업로드 | 2단계 업로드 |
|------|---------------------|-------------|
| **사용처** | 회원가입, 프로필 수정 | 게시글 작성/수정 |
| **요청 횟수** | 1회 (multipart/form-data) | 2회 (POST /images → POST /posts) |
| **트랜잭션** | 원자적 (이미지 포함) | 독립적 (이미지 선행) |
| **UX 장점** | 간편함, 한 번에 완료 | 미리보기, 임시 저장 지원 |
| **핵심 메서드** | AuthService.signup() | PostService.createPost() |

**핵심 구현 패턴:**

**패턴 1 - Multipart 직접 업로드 (AuthService):**
```java
@Transactional
public AuthResponse signup(SignupRequest request, MultipartFile profileImage) {
    // 1. 이메일 중복 확인
    if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
        throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, 
                "Email already exists: " + request.getEmail());
    }
    
    // 2. 닉네임 중복 확인
    if (userRepository.existsByNickname(request.getNickname())) {
        throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS, 
                "Nickname already exists: " + request.getNickname());
    }
    
    // 3. 비밀번호 정책 검증
    if (!PasswordValidator.isValid(request.getPassword())) {
        throw new BusinessException(ErrorCode.INVALID_PASSWORD_POLICY, 
                PasswordValidator.getPolicyDescription());
    }
    
    // 4. 비밀번호 암호화
    String encodedPassword = passwordEncoder.encode(request.getPassword());
    
    // 5. 프로필 이미지 업로드 (있을 경우)
    Image image = null;
    if (profileImage != null &amp;&amp; !profileImage.isEmpty()) {
        ImageResponse imageResponse = imageService.uploadImage(profileImage);
        image = imageRepository.findById(imageResponse.getImageId())
                .orElseThrow(() -&gt; new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        image.clearExpiresAt();  // TTL 해제 (영구 보존)
    }
    
    // 6. User 생성 (DTO 변환 사용)
    User user = request.toEntity(encodedPassword);
    if (image != null) {
        user.updateProfileImage(image);
    }
    userRepository.save(user);
    
    // 7. 자동 로그인 - 토큰 발급
    return generateTokens(user);
}
```

**패턴 2 - 2단계 업로드 (PostService):**
```java
@Transactional
public PostResponse createPost(PostCreateRequest request, Long userId) {
    // ... 게시글 생성 및 저장 ...
    
    // 이미지 연결 (imageId가 있을 경우)
    if (request.getImageId() != null) {
        Image image = imageRepository.findById(request.getImageId())
                .orElseThrow(() -&gt; new BusinessException(ErrorCode.IMAGE_NOT_FOUND,
                        "Image not found with id: " + request.getImageId()));
        
        image.clearExpiresAt();  // TTL 해제 (영구 보존)
        
        PostImage postImage = PostImage.builder()
                .post(savedPost)
                .image(image)
                .displayOrder(1)
                .build();
        postImageRepository.save(postImage);
    }
    
    return PostResponse.from(savedPost);
}
```

**TTL 패턴 (공통 핵심):**
- **업로드 시**: ImageService가 `expires_at = NOW() + 1시간` 설정
- **사용 시**: `image.clearExpiresAt()` 호출 → `expires_at = NULL` (영구 보존)
- **미사용 시**: Phase 4 배치가 expires_at &lt; NOW() 조건으로 S3 + DB 삭제
- **인덱스**: `idx_images_expires` 활용으로 빠른 조회

**설계 결정사항:**
- **검증 로직**: AuthService.signup()에서 이메일/닉네임/비밀번호 검증 모두 구현됨 (생략 아님)
- **User 생성**: Builder 직접 사용 대신 `request.toEntity()` + `updateProfileImage()` 패턴
- **트랜잭션 안전성**: 패턴 1은 완전 원자적, 패턴 2는 이미지만 선행 업로드 (S3 파일 고아 가능)

**참조**: 
- AuthService.signup() - 패턴 1 전체 구현
- PostService.createPost() - 패턴 2 전체 구현
- ImageService.uploadImage() - 공통 검증 로직
- **@docs/API.md Section 2.1, 3.3, 4.1**
- **@docs/DDL.md** (images 테이블)

---

## 8. 예외 처리

### 8.1 예외 처리 구조

**3-Layer 아키텍처:**
- **ErrorCode enum**: 에러 정보 중앙 관리 (28개 에러 코드)
- **BusinessException**: 통합 예외 클래스 (ErrorCode 래핑)
- **GlobalExceptionHandler**: 중앙 예외 처리 (@RestControllerAdvice)

**ErrorCode 형식:** `{DOMAIN}-{NUMBER}` (예: USER-001, POST-001, AUTH-001)

**참조**: `src/main/java/com/ktb/community/enums/ErrorCode.java` (28개 에러 정의)

---

### 8.2 예외 핸들러 목록

| 핸들러 | 예외 타입 | HTTP 상태 | 설명 |
|--------|-----------|-----------|------|
| handleBusinessException | BusinessException | ErrorCode 기반 | 비즈니스 로직 에러 (통합) |
| handleMaxUploadSizeExceeded | MaxUploadSizeExceededException | 413 | 파일 크기 초과 (Phase 3.5) |
| handleValidationException | MethodArgumentNotValidException | 400 | DTO 검증 실패 (@Valid) |
| handleIllegalArgumentException | IllegalArgumentException | 400 | 잘못된 요청 파라미터 |
| handleIllegalStateException | IllegalStateException | 400 | 비즈니스 로직 오류 |
| handleGeneralException | Exception | 500 | 예상하지 못한 서버 오류 |
| handleNullPointerException | NullPointerException | 500 | Null 참조 오류 |

---

### 8.3 핵심 패턴

**통합 예외 처리 (BusinessException):**
```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity&lt;ApiResponse&lt;ErrorDetails&gt;&gt; handleBusinessException(BusinessException ex) {
    ErrorCode errorCode = ex.getErrorCode();
    ErrorDetails errorDetails = ErrorDetails.of(ex.getMessage());
    ApiResponse&lt;ErrorDetails&gt; response = ApiResponse.error(errorCode.getCode(), errorDetails);
    
    return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
}
```

**핵심 장점:**
- ErrorCode enum이 HTTP 상태, 에러 코드, 메시지 모두 관리
- Service Layer에서 `throw new BusinessException(ErrorCode.XXX)` 한 줄로 통일
- GlobalExceptionHandler가 자동 매핑 (ErrorCode → HTTP 상태 + 응답)

**사용 예시:**
```java
// Service Layer
if (!comment.getUser().getUserId().equals(userId)) {
    throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS,
            "Not authorized to update this comment");
}

// 자동 변환: UNAUTHORIZED_ACCESS → HTTP 403 + "COMMON-XXX" + 메시지
```

**참조:** GlobalExceptionHandler.java (전체 핸들러 7개), ErrorCode.java (28개 에러 정의)

---

## 9. 데이터 변환 (Entity ↔ DTO)

**패턴:**
```java
// Entity → DTO
public static PostResponse from(Post post) {
    return PostResponse.builder()
        .postId(post.getPostId())
        .title(post.getTitle())
        .author(UserSummary.from(post.getUser()))
        .stats(PostStatsResponse.from(post.getStats()))
        .build();
}

// DTO → Entity
public Post toEntity(User user) {
    return Post.builder()
        .title(this.title)
        .content(this.content)
        .status(PostStatus.ACTIVE)
        .user(user)
        .build();
}
```

**DTO 검증:** `@NotBlank`, `@Size`, `@Valid` 활용

---

## 10. 설정 파일

**핵심 설정 항목:**

| 항목 | 설정값 | 설명 |
|------|--------|------|
| **HikariCP** | maximum-pool-size: 10 | DB 커넥션 풀 크기 |
| **Multipart** | max-file-size: 5MB, max-request-size: 10MB | 이미지 업로드 제한 |
| **JPA** | ddl-auto: validate, open-in-view: false | 운영 모드, OSIV 비활성화 |
| **JWT** | access: 30분, refresh: 7일 | 토큰 유효기간 |
| **S3** | bucket/region 환경 변수 주입 | AWS 인증 자동 인식 |
| **Rate Limit** | requests-per-minute: 100 | 기본 제한 (엔드포인트별 override 가능) |

**환경 변수 (필수):**
```bash
# Phase 1-2
DB_PASSWORD=<MySQL 비밀번호>
JWT_SECRET=<256bit 이상 시크릿>

# Phase 3.5+ (S3)
AWS_ACCESS_KEY_ID=<AWS Access Key>
AWS_SECRET_ACCESS_KEY=<AWS Secret Key>
AWS_REGION=ap-northeast-2
AWS_S3_BUCKET=<버킷 이름>
```

**참조:** `src/main/resources/application.yaml` (전체 설정)

---

## 11. 테스트 전략

**커버리지 목표:** Service 80%+, Repository 60%+, 전체 60%+

**패턴:**
```java
// Service 테스트
@ExtendWith(MockitoExtension.class)
class PostServiceTest {
    @Mock private PostRepository postRepository;
    @InjectMocks private PostService postService;
    
    @Test
    void createPost_Success() {
        // Given, When, Then + verify()
    }
}

// Repository 테스트
@DataJpaTest
class PostRepositoryTest {
    @Autowired private PostRepository postRepository;
    // 실제 DB 쿼리 테스트
}
```

---

## 12. 성능 최적화

### 12.1 데이터베이스 최적화

**N+1 문제 해결:**
```java
@Query("SELECT p FROM Post p " +
       "JOIN FETCH p.user " +
       "LEFT JOIN FETCH p.stats " +
       "WHERE p.status = :status")
Page<Post> findByStatusWithUserAndStats(...);
```

**인덱스 활용:**
- DDL.md의 인덱스 정의 준수
- EXPLAIN으로 쿼리 실행 계획 분석

### 12.2 캐싱 전략 (추후)

**Redis 도입 시:**
- 토큰 관리: Refresh Token 저장
- 세션 관리: 사용자 세션 캐싱
- 데이터 캐싱: 자주 조회되는 게시글 목록
- TTL 설정: 데이터 특성별 만료 시간

### 12.3 동시성 제어

**문제:** PostStats (좋아요/댓글/조회수) 동시 업데이트 시 Race Condition

**해결:** DB 레벨 원자적 UPDATE

```sql
UPDATE post_stats
SET like_count = like_count + 1,
    last_updated = NOW()
WHERE post_id = ?;
```

**Repository 구현:**
```java
@Modifying(clearAutomatically = true)
@Query("UPDATE PostStats ps SET ps.likeCount = ps.likeCount + 1, " +
       "ps.lastUpdated = CURRENT_TIMESTAMP WHERE ps.postId = :postId")
int incrementLikeCount(@Param("postId") Long postId);

@Modifying(clearAutomatically = true)
@Query("UPDATE PostStats ps SET ps.likeCount = ps.likeCount - 1, " +
       "ps.lastUpdated = CURRENT_TIMESTAMP " +
       "WHERE ps.postId = :postId AND ps.likeCount > 0")
int decrementLikeCount(@Param("postId") Long postId);
```

**적용 대상:**
- `likeCount`: 좋아요 증감
- `commentCount`: 댓글 수 증감  
- `viewCount`: 조회수 증가 (가장 빈번)

**대안 검토:**
- ❌ 낙관적 락 (@Version): 재시도 폭증
- ❌ 비관적 락 (FOR UPDATE): 과도한 오버헤드
- ✅ 원자적 UPDATE: 성능과 일관성 최적

---

## 13. 배포 및 운영

**환경 변수:**
```bash
DB_PASSWORD=<MySQL 비밀번호>
JWT_SECRET=<256bit 이상 시크릿>
```

**배치 작업 (추후):**
- 만료 토큰 정리: 매일 새벽 3시, @Scheduled

**로그 레벨:**
- 운영: INFO, 개발: DEBUG
- 주요 포인트: API 요청/응답, 인증 실패, 비즈니스 에러

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 |
|------|------|-----------|
| 2025-09-30 | 1.0 | 초기 LLD 작성 |
| 2025-10-04 | 1.1 | Claude Code 최적화 (참조 기반, 섹션 재구조화) |
| 2025-10-04 | 1.2 | 핵심 섹션 완전 복원 (6.4, 6.5, 12.3) |
| 2025-10-04 | 1.3 | Section 7.4 댓글 작성 흐름 추가 (참조 무결성 복원) |
| 2025-10-10 | 1.4 | HTML 이스케이프 코드 수정 (Section 6.5) |