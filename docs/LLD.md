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
- DTO 검증 (Bean Validation)
- 요청 → Service 전달, 응답 포매팅
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

```
1. Controller: DTO 검증 (제목 27자, 내용 필수)
2. Service: 인증 사용자 확인
3. Service: Post 엔티티 생성 (상태: ACTIVE)
4. Repository: Post 저장
5. Repository: PostStats 초기화 (카운트 0)
6. Service: 이미지 있으면 연결
7. Controller: 201 Created + PostResponse
```

**핵심 패턴:**
```java
@Transactional
public PostResponse createPost(PostCreateRequest request, Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    Post post = request.toEntity(user);
    Post saved = postRepository.save(post);
    
    // 통계 초기화
    postStatsRepository.save(PostStats.builder()
        .post(saved).likeCount(0).commentCount(0).viewCount(0).build());
    
    // 이미지 연결 (image_id가 있을 경우)
    if (request.getImageId() != null) {
        Image image = imageRepository.findById(request.getImageId())
            .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        
        // expires_at 클리어 (영구 보존)
        image.clearExpiresAt();
        
        PostImage postImage = PostImage.builder()
            .post(saved)
            .image(image)
            .displayOrder(1)
            .build();
        postImageRepository.save(postImage);
    }
    
    return PostResponse.from(saved);
}
```

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

```
1. Controller: DTO 검증 (내용 200자)
2. Service: 게시글 존재 확인
3. Service: 인증 사용자 확인
4. Service: Comment 엔티티 생성 (상태: ACTIVE)
5. Repository: Comment 저장
6. Repository: PostStats.commentCount 원자적 증가 (Section 12.3)
7. Controller: 201 Created + CommentResponse
```

**핵심 패턴:**
```java
@Transactional
public CommentResponse createComment(Long postId, CommentCreateRequest request, Long userId) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    Comment comment = request.toEntity(post, user);
    Comment saved = commentRepository.save(comment);
    
    // 댓글 수 자동 증가 (Section 12.3 동시성 제어)
    postStatsRepository.incrementCommentCount(postId);
    
    return CommentResponse.from(saved);
}
```

**권한 검증 (수정/삭제):**
```java
@Transactional
public CommentResponse updateComment(Long commentId, CommentUpdateRequest request, Long userId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
    
    // 작성자 본인만 수정 가능
    if (!comment.getUser().getUserId().equals(userId)) {
        throw new ForbiddenException("Not authorized to update this comment");
    }
    
    comment.updateContent(request.getContent());
    return CommentResponse.from(comment);
}
```

**Soft Delete 처리:**
```java
@Transactional
public void deleteComment(Long commentId, Long userId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
    
    if (!comment.getUser().getUserId().equals(userId)) {
        throw new ForbiddenException("Not authorized to delete this comment");
    }
    
    comment.updateStatus(CommentStatus.DELETED);
    postStatsRepository.decrementCommentCount(comment.getPost().getPostId());
}
```

**참조**: **@docs/API.md Section 5** (댓글 API), **@docs/DDL.md** (comments 테이블)

---

### 7.5 이미지 업로드 흐름 (S3 직접 연동)

**2가지 업로드 패턴:**

#### 패턴 1: Multipart 직접 업로드 (회원가입/프로필 수정)

```
Client → POST /users/signup (multipart/form-data)
    ↓ email, password, nickname, profile_image
AuthController → @RequestPart로 파일 수신
    ↓
AuthService.signup(SignupRequest, MultipartFile)
    ↓
ImageService.uploadImage(file) 호출
    ↓ 파일 검증 (MIME + Magic Number)
    ↓ S3 업로드
    ↓ DB 저장 (expires_at = 1시간)
    ↓ image.clearExpiresAt() (영구 보존)
    ↓
User 엔티티 생성 (profileImage 설정)
    ↓
Client ← { access_token, refresh_token }
```

**장점:**
- 자연스러운 UX (한 번의 요청으로 완료)
- 원자적 트랜잭션 (회원가입 실패 시 이미지도 롤백)
- 프론트엔드 구현 단순화

**적용 대상:**
- POST /users/signup
- PATCH /users/{userId} (프로필 수정)

---

#### 패턴 2: 2단계 업로드 (게시글 작성)

```
Phase 1: 이미지 업로드
Client → POST /images (multipart/form-data)
    ↓
ImageService → 파일 검증 (크기, 형식, Magic Number)
    ↓
S3Client → AWS S3 업로드
    ↓
ImageRepository → DB 저장 (expires_at = 1시간 후)
    ↓
Client ← { image_id: 123, image_url: "https://..." }

Phase 2: 게시글 작성
Client → POST /posts { "title": "...", "image_id": 123 }
    ↓
PostService → image_id 검증
    ↓
Image → expires_at 클리어 (영구 보존)
    ↓
PostImageRepository → 연결 테이블 저장
```

**장점:**
- 이미지 미리보기 가능
- 드래그앤드롭, 복수 이미지 지원 용이
- 임시 저장 기능 지원
- 이미지와 컨텐츠 독립적 관리

**적용 대상:**
- POST /posts (게시글 작성)
- PATCH /posts/{postId} (게시글 수정)

---

**핵심 구현 패턴:**

```java
// 패턴 1: Multipart 직접 업로드
@Service
public class AuthService {
    private final ImageService imageService;
    private final ImageRepository imageRepository;
    
    @Transactional
    public AuthResponse signup(SignupRequest request, MultipartFile profileImage) {
        // 이메일/닉네임/비밀번호 검증 생략
        
        // 프로필 이미지 업로드 (있을 경우)
        Image image = null;
        if (profileImage != null && !profileImage.isEmpty()) {
            ImageResponse imageResponse = imageService.uploadImage(profileImage);
            image = imageRepository.findById(imageResponse.getImageId())
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
            image.clearExpiresAt();  // 영구 보존
        }
        
        // User 생성
        User user = User.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .nickname(request.getNickname())
            .profileImage(image)  // null 가능
            .build();
        
        userRepository.save(user);
        
        // 토큰 발급 및 반환
        // ...
    }
}

// 패턴 2: 2단계 업로드
@Service
public class PostService {
    @Transactional
    public PostResponse createPost(PostCreateRequest request, Long userId) {
        // 게시글 생성 로직 생략
        
        // 이미지 연결 (있을 경우)
        if (request.getImageId() != null) {
            Image image = imageRepository.findById(request.getImageId())
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
            
            image.clearExpiresAt();  // 영구 보존
            
            PostImage postImage = PostImage.builder()
                .post(savedPost)
                .image(image)
                .displayOrder(1)
                .build();
            postImageRepository.save(postImage);
        }
        
        return PostResponse.from(savedPost);
    }
}
```

**고아 이미지 처리 (Phase 4):**
- **업로드 시**: `expires_at = NOW() + 1시간` 설정
- **사용 시**: `image.clearExpiresAt()` 호출로 expires_at = NULL (영구 보존)
- **배치 작업**: 매일 새벽 expires_at < NOW() 조건으로 S3 + DB 삭제
- **인덱스**: `idx_images_expires` 활용으로 빠른 조회

**참조**: **@docs/API.md Section 4.1** (이미지 업로드 API), **@docs/DDL.md** (images 테이블)

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

**BusinessException (통합 에러 처리):**
```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ApiResponse<ErrorDetails>> handleBusinessException(BusinessException ex) {
    ErrorCode errorCode = ex.getErrorCode();
    log.warn("Business exception: {} - {}", errorCode.getCode(), ex.getMessage());
    
    ErrorDetails errorDetails = ErrorDetails.of(ex.getMessage());
    ApiResponse<ErrorDetails> response = ApiResponse.error(errorCode.getCode(), errorDetails);
    
    return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
}
```

**MaxUploadSizeExceededException (Phase 3.5 추가 예정):**
```java
/**
 * 파일 크기 초과 예외 처리 (서버 레벨)
 * Spring Boot multipart max-file-size 초과 시 발생
 * IMAGE-002 에러 코드로 통일하여 클라이언트에 일관된 응답 제공
 */
@ExceptionHandler(MaxUploadSizeExceededException.class)
public ResponseEntity<ApiResponse<ErrorDetails>> handleMaxUploadSizeExceeded(
        MaxUploadSizeExceededException ex) {
    
    log.warn("File size exceeded: {}", ex.getMessage());
    
    ErrorDetails errorDetails = ErrorDetails.of("File size exceeds 5MB limit");
    ApiResponse<ErrorDetails> response = ApiResponse.error(
        ErrorCode.FILE_TOO_LARGE.getCode(), errorDetails);
    
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
}
```

**DTO 검증 실패 (Bean Validation):**
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ApiResponse<ErrorDetails>> handleValidationException(
        MethodArgumentNotValidException ex) {
    
    String details = ex.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining(", "));
    
    ErrorDetails errorDetails = ErrorDetails.of(details);
    ApiResponse<ErrorDetails> response = ApiResponse.error(
        ErrorCode.INVALID_INPUT.getCode(), errorDetails);
    
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
}
```

**사용 예시:**
```java
// Service Layer
if (userRepository.existsByEmail(email)) {
    throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
}

// 자동 변환: EMAIL_ALREADY_EXISTS → HTTP 409 + "USER-002" 응답
```

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

**application.yaml 핵심:**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/community
    username: root
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
  servlet:
    multipart:
      max-file-size: 5MB        # 단일 이미지 파일 크기 제한
      max-request-size: 10MB    # 전체 요청 크기 제한 (향후 다중 이미지 대비)
      enabled: true
  jpa:
    hibernate:
      ddl-auto: validate  # 운영: validate
    open-in-view: false   # OSIV 비활성화

jwt:
  secret: ${JWT_SECRET}
  access-token-validity: 1800000   # 30분
  refresh-token-validity: 604800000 # 7일

# AWS S3 설정 (Phase 3.5+)
aws:
  s3:
    bucket: ${AWS_S3_BUCKET}              # S3 버킷 이름
    region: ${AWS_REGION:ap-northeast-2}  # AWS 리전 (기본: 서울)
    # AWS SDK는 AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY 환경 변수 자동 인식

rate-limit:
  requests-per-minute: 100
```

**환경 변수:**
- **Phase 1-2 (필수)**: `DB_PASSWORD`, `JWT_SECRET`
- **Phase 3.5+ (필수)**: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `AWS_S3_BUCKET`

**참고**: AWS SDK는 표준 환경 변수를 자동 인식하므로 application.yaml에 명시적으로 credential을 넣지 않음

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