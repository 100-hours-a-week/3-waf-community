-- 이미지 저장 테이블
CREATE TABLE images (
    image_id BIGINT AUTO_INCREMENT,
image_url VARCHAR(2048) NOT NULL,
file_size INT UNSIGNED,              
original_filename VARCHAR(255),      
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
expires_at TIMESTAMP NULL DEFAULT NULL,  -- 고아 이미지 관리용 (Phase 4 배치에서 사용)

    PRIMARY KEY(image_id),
    KEY idx_images_expires (expires_at)     -- 만료된 이미지 조회용 인덱스
);

-- 유저 테이블
CREATE TABLE users (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
email        VARCHAR(255) NOT NULL,
password_hash VARCHAR(255) NOT NULL,
nickname     VARCHAR(30)  NOT NULL, -- 10자 제한(한글 기준)
role VARCHAR(20) NOT NULL DEFAULT 'USER',
user_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
image_id BIGINT DEFAULT NULL, -- 프로필 이미지(옵션, 1:1)

PRIMARY KEY (user_id),
UNIQUE KEY uq_users_email (email), -- 이메일 중복 확인
UNIQUE KEY uq_users_nickname (nickname), -- 닉네임 중복 확인
KEY idx_users_status (user_status), -- 상태별 조회용 인덱스
CONSTRAINT fk_users_profile_image
FOREIGN KEY (image_id) REFERENCES images(image_id)
ON DELETE SET NULL
ON UPDATE RESTRICT,
CONSTRAINT chk_users_nickname_len -- 닉네임 길이 제한
CHECK (CHAR_LENGTH(nickname) <= 10),
CONSTRAINT chk_users_role -- 역할 값 검증
CHECK (role IN ('USER', 'ADMIN')),
CONSTRAINT chk_users_status -- 상태 값 검증
CHECK (user_status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
CONSTRAINT chk_users_email_trim_lower -- 이메일 공백 제거 및 소문자 변환
CHECK (email = LOWER(TRIM(email))),
CONSTRAINT chk_users_email_no_ws -- 이메일 내부 공백 문자 포함 금지
CHECK (email NOT REGEXP '\\s')  
);

-- 게시글 테이블
CREATE TABLE posts (
    post_id BIGINT AUTO_INCREMENT,
post_title VARCHAR(100) NOT NULL, -- 제목 27자 제한(한글 기준)
post_content LONGTEXT NOT NULL,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
post_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 상태 값 (임시 저장 상태 고려)
    user_id BIGINT NOT NULL, -- NULL 불가: API가 항상 author 객체 반환

    PRIMARY KEY (post_id),
    KEY idx_posts_created (created_at DESC), -- 최신 글 조회용 인덱스
    KEY idx_posts_user_created (user_id, created_at DESC), -- 유저별 최신 글 조회용 인덱스
    CONSTRAINT fk_posts_user
      FOREIGN KEY (user_id) REFERENCES users(user_id)
      ON DELETE RESTRICT -- 게시글이 있는 사용자는 삭제 불가 (user_status로 관리)
      ON UPDATE RESTRICT,
    CONSTRAINT chk_posts_title_len -- 제목 길이 제한
      CHECK (CHAR_LENGTH(post_title) <= 27),
    CONSTRAINT chk_posts_status -- 상태 값 검증
      CHECK (post_status IN ('ACTIVE', 'DELETED', 'DRAFT'))
);

-- 게시글 통계 테이블
CREATE TABLE post_stats (
    post_id BIGINT,
like_count INT UNSIGNED NOT NULL DEFAULT 0,
comment_count INT UNSIGNED NOT NULL DEFAULT 0,
view_count INT UNSIGNED NOT NULL DEFAULT 0,
last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (post_id),
    CONSTRAINT fk_post_stats_post 
      FOREIGN KEY (post_id) REFERENCES posts(post_id) 
      ON DELETE CASCADE
      ON UPDATE RESTRICT
);

-- 댓글 테이블
CREATE TABLE comments (
    comment_id BIGINT AUTO_INCREMENT,
comment_content VARCHAR(600) NOT NULL, -- 댓글 200자 제한(한글 기준)
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
comment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 상태 값
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL, -- NULL 불가: API가 항상 author 객체 반환

    PRIMARY KEY (comment_id),
    KEY idx_comments_post_created (post_id, created_at, comment_id), -- 특정 게시글의 댓글 조회용 인덱스(생성순)
    CONSTRAINT fk_comments_post
      FOREIGN KEY (post_id) REFERENCES posts(post_id)
      ON DELETE CASCADE
      ON UPDATE RESTRICT,
    CONSTRAINT fk_comments_user
      FOREIGN KEY (user_id) REFERENCES users(user_id)
      ON DELETE RESTRICT -- 댓글이 있는 사용자는 삭제 불가 (user_status로 관리)
      ON UPDATE RESTRICT,
    CONSTRAINT chk_comments_len -- 댓글 길이 제한
      CHECK (CHAR_LENGTH(comment_content) <= 200),
    CONSTRAINT chk_comments_status -- 상태 값 검증
      CHECK (comment_status IN ('ACTIVE', 'DELETED'))
);

-- 게시글 이미지 브릿지 테이블(순서 관리 포함)
CREATE TABLE post_images (
    post_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
display_order TINYINT UNSIGNED NOT NULL DEFAULT 1,

    PRIMARY KEY (post_id, image_id),
    UNIQUE KEY uq_post_images_order (post_id, display_order), -- 순서 중복 방지
    CONSTRAINT fk_post_images_post 
      FOREIGN KEY (post_id) REFERENCES posts(post_id) 
      ON DELETE CASCADE
      ON UPDATE RESTRICT,
    CONSTRAINT fk_post_images_image
      FOREIGN KEY (image_id) REFERENCES images(image_id)
      ON DELETE CASCADE
      ON UPDATE RESTRICT
);

-- 게시글 좋아요 테이블
CREATE TABLE post_likes (
    like_id BIGINT AUTO_INCREMENT, -- 단일 PK
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (like_id),
    UNIQUE KEY uq_user_post (user_id, post_id), -- 중복 방지
    KEY idx_post_likes_post (post_id), -- 게시글별 조회
    CONSTRAINT fk_post_likes_user 
      FOREIGN KEY (user_id) REFERENCES users(user_id)
      ON DELETE CASCADE
      ON UPDATE RESTRICT,
    CONSTRAINT fk_post_likes_post 
      FOREIGN KEY (post_id) REFERENCES posts(post_id)
      ON DELETE CASCADE
      ON UPDATE RESTRICT
);

-- 사용자 토큰 테이블
CREATE TABLE user_tokens (
    user_token_id BIGINT NOT NULL AUTO_INCREMENT,
token         VARCHAR(512) NOT NULL,
expires_at    TIMESTAMP NOT NULL,
created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
user_agent    VARCHAR(255),
ip_address VARCHAR(45), -- IPv6 대응
    user_id BIGINT NOT NULL,

PRIMARY KEY (user_token_id),
UNIQUE KEY uq_user_tokens_token (token), -- 토큰 중복 방지
KEY idx_user_tokens_user (user_id), -- 사용자별 토큰 조회용 인덱스
KEY idx_tokens_expires (expires_at), -- 만료된 토큰 정리용 인덱스
CONSTRAINT fk_tokens_user
FOREIGN KEY (user_id) REFERENCES users(user_id)
ON DELETE CASCADE
ON UPDATE RESTRICT
);
