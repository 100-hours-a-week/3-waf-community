# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

Spring Boot 3.5.6 community platform (Java 24, MySQL 8.0+, JPA/Hibernate)  
Package: `com.ktb.community`

---

## 문서 구조
**@CLAUDE.md**(현재 파일) - 빠른 참조 가이드
    ↓
**@docs/PLAN.md** - Phase별 구현 로드맵 (Week 1-7)요구사항 
    ↓
**@docs/PRD.md** - (FR/NFR 코드)
    ↓
**@docs/LLD.md** - 설계 (아키텍처, 패턴)
    ↓
**@docs/API.md** + **@docs/DDL.md** - 스키마, 엔드포인트

---

## 개발 워크플로우

### 1. 작업 시작 전
```bash
# Phase 확인
@docs/PLAN.md → 현재 Phase와 체크리스트 확인

# 요구사항 확인
@docs/PRD.md → FR/NFR 코드로 비즈니스 요구사항 파악

# 설계 확인
@docs/LLD.md → 구현 패턴 확인
```

### 2. 개발 중
```bash
# DB 스키마
@docs/DDL.md

# API 스펙
@docs/API.md

# 실제 코드
@src/main/java/com/ktb/community/
```

### 3. 커밋 전
```bash
# FR 코드 포함
EX) git commit -m "feat: FR-POST-001 게시글 작성 API 구현"

# Phase 진행률 업데이트
@docs/PLAN.md 체크박스 수정
```

---

## 핵심 개발 원칙

**3-Layer Architecture 엄수:**
- Controller: DTO 검증, 요청/응답 처리
- Service: 비즈니스 로직, @Transactional
- Repository: 데이터 접근, JPA

**코드 통일성 패턴:**
- Entity ↔ DTO: `from()`, `toEntity()` 메서드
- 예외: CustomException 계층 사용
- 응답: ApiResponse 표준 구조

**성능 체크리스트:**
- [ ] N+1 방지 (FETCH JOIN)
- [ ] 페이지네이션 (웹: Offset/Limit, 모바일: Cursor)
- [ ] 동시성 제어 (원자적 UPDATE: 좋아요/조회수/댓글수)

**테스트:**
- Phase별 단위 테스트 필수 (Service Layer 80%+)
- Phase 완료 전 모든 테스트 통과 확인

---

## 제약사항 (설계 배경)

**기술:**
- 토큰: RDB 저장 (user_tokens 테이블) → 추후 Redis
- 이미지: URL만 저장 → 추후 S3

**데이터:**
- Soft Delete: User, Post, Comment (status 변경)
- Hard Delete: UserToken (배치)

**현재 가정:**
- 초기 트래픽 낮음 → 단일 서버 충분
- Phase 완료 후 고도화 (PLAN.md Phase 6+)

---

## Essential Commands

```bash
# Build & Run
./gradlew bootRun
./gradlew build

# Test
./gradlew test
./gradlew test --tests ClassName.methodName

# Database
mysql -u root -p community  # MySQL 접속
```

**Database:**
- URL: jdbc:mysql://localhost:3306/community
- DDL mode: update (자동 스키마 수정)
- 환경 변수: DB_PASSWORD, JWT_SECRET

---

## 빠른 참조

| 필요한 정보 | 문서 |
|------------|------|
| 현재 Phase 확인 | @docs/PLAN.md |
| FR 코드 찾기 | @docs/PRD.md |
| 구현 패턴 | @docs/LLD.md Section 7 |
| 테이블 구조 | @docs/DDL.md |
| API 엔드포인트 | @docs/API.md |
| 동시성 제어 | @docs/LLD.md Section 12.3 |
| 예외 처리 | @docs/LLD.md Section 8 |

---

## MCP Documentation
@Users/jsh/.claude/MCP_Context7.md