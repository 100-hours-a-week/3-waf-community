-- H2 테스트용 스키마 (MySQL DDL.md 기반, ENUM → VARCHAR 변환)

-- 이미지 저장 테이블
CREATE TABLE images (
    image_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_url VARCHAR(2048) NOT NULL,
    file_size INT,
    original_filename VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NULL
);

CREATE INDEX idx_images_expires ON images(expires_at);

-- 유저 테이블
CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(30) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    user_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    image_id BIGINT,
    CONSTRAINT fk_users_profile_image FOREIGN KEY (image_id) REFERENCES images(image_id) ON DELETE SET NULL,
    CONSTRAINT chk_users_nickname_len CHECK (CHAR_LENGTH(nickname) <= 10)
);

CREATE INDEX idx_users_status ON users(user_status);

-- 게시글 테이블
CREATE TABLE posts (
    post_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_title VARCHAR(100) NOT NULL,
    post_content LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    post_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT chk_posts_title_len CHECK (CHAR_LENGTH(post_title) <= 27)
);

CREATE INDEX idx_posts_created ON posts(created_at DESC);
CREATE INDEX idx_posts_user_created ON posts(user_id, created_at DESC);

-- 게시글 통계 테이블
CREATE TABLE post_stats (
    post_id BIGINT PRIMARY KEY,
    like_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    view_count INT NOT NULL DEFAULT 0,
    last_updated TIMESTAMP NOT NULL,
    CONSTRAINT fk_post_stats_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE
);

-- 댓글 테이블
CREATE TABLE comments (
    comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_content VARCHAR(600) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    comment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT chk_comments_len CHECK (CHAR_LENGTH(comment_content) <= 200)
);

CREATE INDEX idx_comments_post_created ON comments(post_id, created_at, comment_id);

-- 게시글 이미지 브릿지 테이블
CREATE TABLE post_images (
    post_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    display_order TINYINT NOT NULL DEFAULT 1,
    PRIMARY KEY (post_id, image_id),
    CONSTRAINT fk_post_images_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE,
    CONSTRAINT fk_post_images_image FOREIGN KEY (image_id) REFERENCES images(image_id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_post_images_order ON post_images(post_id, display_order);

-- 게시글 좋아요 테이블
CREATE TABLE post_likes (
    like_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_post_likes_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_post_likes_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_user_post ON post_likes(user_id, post_id);
CREATE INDEX idx_post_likes_post ON post_likes(post_id);

-- 사용자 토큰 테이블
CREATE TABLE user_tokens (
    user_token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    user_agent VARCHAR(255),
    ip_address VARCHAR(45),
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_tokens_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_user_tokens_user ON user_tokens(user_id);
CREATE INDEX idx_tokens_expires ON user_tokens(expires_at);
