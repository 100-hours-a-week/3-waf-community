# 더미 데이터 생성 및 실행 가이드

이 디렉토리에는 KTB Community 프로젝트의 더미 데이터를 생성하고 관리하는 스크립트가 포함되어 있습니다.

## 📋 데이터 구성

| 항목 | 개수 | 설명 |
|------|------|------|
| **유저** | 1200명 | 한글 이름, Test1234! 비밀번호 (모두 동일) |
| **게시글** | 3000개 | 한글 제목/내용, 최근 6개월 내 랜덤 날짜 |
| **댓글** | 30K~45K개 | 게시글당 10~15개 랜덤 배치 |
| **좋아요** | 1200개 | post_id=3000에 모든 유저가 좋아요 |

---

## 🚀 실행 방법

### 1. SQL 파일 생성

더미 데이터 SQL 파일을 생성하려면:

```bash
# 프로젝트 루트 디렉토리에서
./gradlew generateDummyData
```

**출력 파일**: `scripts/dummy_data.sql`
**실행 시간**: 약 0.2~0.5초

---

### 2. MySQL에 데이터 삽입

#### 방법 1: MySQL CLI 사용

```bash
# MySQL 접속
mysql -u root -p community

# SQL 파일 실행
source /절대경로/scripts/dummy_data.sql;
```

#### 방법 2: 한 줄 명령어

```bash
mysql -u root -p community < scripts/dummy_data.sql
```

**⚠️ 주의사항:**
- 외래 키 제약 조건이 자동으로 비활성화/활성화됩니다 (`SET FOREIGN_KEY_CHECKS`)
- 기존 데이터와 ID가 충돌할 수 있으므로, 깨끗한 데이터베이스에 실행하는 것을 권장합니다
- 실행 시간: 약 5~10초 (데이터 양에 따라 다름)

---

## ✅ 데이터 검증

삽입된 데이터를 확인하려면:

```sql
-- MySQL CLI에서
USE community;

-- 테이블별 데이터 개수 확인
SELECT 'users' AS table_name, COUNT(*) AS count FROM users
UNION ALL
SELECT 'posts', COUNT(*) FROM posts
UNION ALL
SELECT 'post_stats', COUNT(*) FROM post_stats
UNION ALL
SELECT 'comments', COUNT(*) FROM comments
UNION ALL
SELECT 'post_likes', COUNT(*) FROM post_likes;

-- 예상 결과:
-- users: 1200
-- posts: 3000
-- post_stats: 3000
-- comments: 30000~45000
-- post_likes: 1200
```

### 특정 데이터 확인

```sql
-- 모든 유저가 좋아요한 게시글 (post_id=3000)
SELECT p.post_id, p.post_title, ps.like_count
FROM posts p
JOIN post_stats ps ON p.post_id = ps.post_id
WHERE p.post_id = 3000;

-- 예상: like_count = 1200

-- 샘플 유저 로그인 테스트
SELECT email, nickname
FROM users
LIMIT 5;

-- 모든 유저의 비밀번호: Test1234!
```

---

## 🗑️ 데이터 삭제

**경고**: 이 작업은 되돌릴 수 없습니다!

### 방법 1: 더미 데이터만 삭제 (권장)

```sql
-- 외래 키 제약 조건 임시 비활성화
SET FOREIGN_KEY_CHECKS = 0;

-- 데이터 삭제 (역순으로)
DELETE FROM post_likes;
DELETE FROM comments;
DELETE FROM post_stats;
DELETE FROM posts;
DELETE FROM users;

-- 외래 키 제약 조건 재활성화
SET FOREIGN_KEY_CHECKS = 1;

-- AUTO_INCREMENT 초기화 (선택)
ALTER TABLE users AUTO_INCREMENT = 1;
ALTER TABLE posts AUTO_INCREMENT = 1;
ALTER TABLE comments AUTO_INCREMENT = 1;
ALTER TABLE post_likes AUTO_INCREMENT = 1;
```

### 방법 2: 전체 데이터베이스 초기화 (주의!)

```sql
-- 데이터베이스 삭제 및 재생성
DROP DATABASE community;
CREATE DATABASE community;

-- DDL 스크립트 재실행 필요
-- (docs/be/DDL.md 참조)
```

---

## 🔑 로그인 정보

생성된 모든 유저는 동일한 비밀번호를 사용합니다:

- **비밀번호**: `Test1234!`
- **이메일 형식**: `{성}{이름}{숫자}@test.com` (예: `김민준123@test.com`)
- **닉네임 형식**: `{성}{이름}` (예: `김민준`, 중복 시 숫자 추가)

**샘플 로그인 예시:**

```bash
# 임의의 유저 이메일 조회
mysql -u root -p -e "SELECT email FROM community.users LIMIT 10;"

# 조회된 이메일로 로그인
# 이메일: (위에서 조회한 이메일)
# 비밀번호: Test1234!
```

---

## 🛠️ 재생성 방법

데이터를 다시 생성하려면:

1. 기존 데이터 삭제 (위의 "데이터 삭제" 섹션 참조)
2. SQL 파일 재생성: `./gradlew generateDummyData`
3. SQL 파일 실행: `source scripts/dummy_data.sql;`

**참고:** 매번 생성 시 비밀번호 해시가 달라지므로, 기존 유저 로그인이 불가능해집니다.

---

## 📝 트러블슈팅

### 문제 1: "Foreign key constraint fails" 오류

**원인**: 외래 키 제약 조건 충돌

**해결**:
```sql
SET FOREIGN_KEY_CHECKS = 0;
-- SQL 파일 다시 실행
SET FOREIGN_KEY_CHECKS = 1;
```

### 문제 2: "Duplicate entry" 오류

**원인**: 기존 데이터와 ID 충돌

**해결**:
- 방법 1: 기존 데이터 삭제 후 재실행
- 방법 2: AUTO_INCREMENT 초기화 후 재실행

### 문제 3: "File not found" 오류 (source 명령)

**원인**: 상대 경로 사용

**해결**:
```bash
# 절대 경로 사용
source /Users/jsh/IdeaProjects/community/scripts/dummy_data.sql;

# 또는 프로젝트 루트에서
source $(pwd)/scripts/dummy_data.sql;
```

---

## 📚 추가 정보

- **유틸리티 소스 코드**: `src/main/java/com/ktb/community/util/DummyDataSqlGenerator.java`
- **Gradle 태스크**: `build.gradle:73` (`generateDummyData`)
- **프로젝트 문서**:
  - `docs/be/DDL.md` (데이터베이스 스키마)
  - `docs/be/API.md` (API 명세)
  - `docs/be/PLAN.md` (구현 계획)

---

## 🔍 데이터 특징

- **유저 이름**: 한국에서 흔한 이름 40개 조합 (성 20개 × 이름 40개)
- **게시글 제목**: 20가지 템플릿 + 번호
- **게시글 내용**: 10가지 템플릿 (커뮤니티 성격에 맞는 한글 문장)
- **댓글**: 20가지 템플릿 (공감, 감사, 응원 등)
- **날짜 분포**:
  - 유저: 최근 1년 내 랜덤
  - 게시글: 최근 6개월 내 랜덤
  - 댓글: 최근 90일 내 랜덤
  - 좋아요: 최근 30일 내 랜덤

---

**마지막 업데이트**: 2025-10-23
**버전**: 1.0
