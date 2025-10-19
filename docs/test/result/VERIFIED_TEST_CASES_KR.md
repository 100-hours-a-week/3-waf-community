# 검증 완료 테스트 케이스 (한국어 버전)

## 문서 정보

| 항목 | 내용 |
|------|------|
| 문서 유형 | QA 검증 완료 테스트 케이스 (한국어) |
| 작성일 | 2025-10-19 |
| 검증 기간 | 2025-10-17 ~ 2025-10-18 |
| 검증 기준 | 완전 검증된 케이스만 포함 |
| 총 케이스 수 | 13개 |

---

## 검증 완료 케이스 개요

| 기능 | 케이스 수 | 번호 |
|------|-----------|------|
| 인증 | 1개 | 1.1 |
| 사용자 관리 | 3개 | 2.1 ~ 2.3 |
| 게시글 | 4개 | 3.1 ~ 3.4 |
| 댓글 | 4개 | 4.1 ~ 4.4 |
| 이미지 | 1개 | 5.1 |
| **합계** | **13개** | |

---

## 1. 인증 (Authentication)

| 기능 | 번호 | 테스트 케이스 | 전제 조건 | 테스트 순서 | 예상 결과 | 실제 결과 | 비고 |
|------|------|---------------|-----------|-------------|-----------|-----------|------|
| 인증 | 1.1 | 로그인 | - 사용자 계정 존재<br>  (email: test@startupcode.kr)<br>- 비밀번호 해시 일치 | 1. /user/login.html 접속<br>2. 이메일 입력: test@startupcode.kr<br>3. 비밀번호 입력: Test1234!<br>4. "로그인" 버튼 클릭<br>5. localStorage 토큰 저장 확인<br>6. /board/list.html 리다이렉트 확인 | HTTP 200 OK<br>- accessToken 발급 (JWT 형식)<br>- refreshToken 발급 (JWT 형식)<br>- localStorage 저장 성공<br>- 게시글 목록 페이지 리다이렉트 | HTTP 200 OK<br>- accessToken 길이: 185자<br>- refreshToken 길이: 127자<br>- localStorage 저장 성공<br>- 리다이렉트 성공 | 검증일: 2025-10-18<br>커밋: a49ebcc<br>(필드명 수정) |

---

## 2. 사용자 관리 (User Management)

| 기능 | 번호 | 테스트 케이스 | 전제 조건 | 테스트 순서 | 예상 결과 | 실제 결과 | 비고 |
|------|------|---------------|-----------|-------------|-----------|-----------|------|
| 사용자 관리 | 2.1 | 회원가입 | - 이메일 미사용 상태<br>- 닉네임 미사용 상태 | 1. /user/register.html 접속<br>2. 폼 입력<br>  - 이메일: test@startupcode.kr<br>  - 비밀번호: Test1234!<br>  - 닉네임: 테스터<br>  - 프로필 이미지: 선택 (JPG/PNG/GIF, 5MB 이하)<br>3. "회원가입" 버튼 클릭<br>4. DB users 테이블 insert 확인<br>5. 프로필 이미지 S3 업로드 확인 (선택 시)<br>6. /board/list.html 리다이렉트 확인 | HTTP 201 Created<br>- accessToken, refreshToken 발급<br>- user_id 생성<br>- 프로필 이미지 S3 업로드 (선택)<br>- 자동 로그인<br>- 게시글 목록 페이지 리다이렉트 | HTTP 201 Created<br>- user_id=11 생성<br>- 토큰 정상 발급<br>- 프로필 이미지 업로드 성공 (선택 시)<br>- 자동 로그인 성공 | 검증일: 2025-10-18<br>요청 형식: multipart/form-data |
| 사용자 관리 | 2.2 | 프로필 조회 | - 사용자 존재<br>  (user_id=11) | 1. /users/11 API 호출<br>2. 응답 데이터 확인<br>  - userId, email, nickname, profileImage 필드 존재<br>3. 프로필 이미지 URL 유효성 검증 | HTTP 200 OK<br>- userId, email, nickname, profileImage 반환<br>- profileImage null (미설정 시) | HTTP 200 OK<br>- 모든 필드 정상 반환<br>- profileImage null (미설정 시) | 검증일: 2025-10-18<br>공개 프로필 조회 |
| 사용자 관리 | 2.3 | 비밀번호 변경 | - 로그인 완료<br>  (본인 계정) | 1. /user/password-change.html 접속<br>2. 새 비밀번호 입력: NewPass1234!<br>3. 새 비밀번호 확인 입력: NewPass1234!<br>4. "변경" 버튼 클릭<br>5. DB password_hash 업데이트 확인<br>6. 로그아웃 후 새 비밀번호로 재로그인 시도 | HTTP 200 OK<br>- 비밀번호 정책 검증 (8-20자, 대/소/특수문자 각 1개+)<br>- password_hash 업데이트<br>- 새 비밀번호로 재로그인 가능 | HTTP 200 OK<br>- 비밀번호 변경 성공<br>- 새 비밀번호로 로그인 확인 | 검증일: 2025-10-18<br>커밋: d7c7b61<br>(필드명 수정) |

---

## 3. 게시글 (Posts)

| 기능 | 번호 | 테스트 케이스 | 전제 조건 | 테스트 순서 | 예상 결과 | 실제 결과 | 비고 |
|------|------|---------------|-----------|-------------|-----------|-----------|------|
| 게시글 | 3.1 | 목록 조회<br>(Cursor) | - DB에 게시글<br>  0개 이상 | 1. /board/list.html 접속<br>2. GET /posts?cursor=null&limit=10&sort=latest 호출<br>3. 게시글 카드 렌더링 확인<br>  (제목, 작성자, 날짜, 통계)<br>4. "더보기" 버튼 클릭 (hasMore=true 시)<br>5. nextCursor로 다음 페이지 요청 | HTTP 200 OK<br>- posts 배열 반환<br>- nextCursor, hasMore 필드 존재<br>- Cursor 페이지네이션 동작<br>- 게시글 렌더링 성공 | HTTP 200 OK<br>- Cursor 페이지네이션 정상<br>- nextCursor=null, hasMore=false (데이터 부족)<br>- 게시글 카드 렌더링 성공 | 검증일: 2025-10-18<br>latest 정렬 전용 |
| 게시글 | 3.2 | 게시글 작성 | - 로그인 완료<br>- (선택) 이미지 업로드 완료<br>  (imageId=123) | 1. /board/write.html 접속<br>2. 제목 입력: "Phase 4 테스트 게시글"<br>3. 내용 입력: "이미지 포함 게시글 테스트"<br>4. 이미지 선택 (선택 사항)<br>5. "작성완료" 버튼 클릭<br>6. DB posts, post_stats 테이블 insert 확인<br>7. 이미지 연결 (post_images) 확인<br>8. 이미지 TTL 해제 (expires_at → NULL) 확인<br>9. /board/detail.html?id={postId} 리다이렉트 | HTTP 201 Created<br>- postId 반환<br>- DB posts 테이블 insert<br>- post_stats 초기화 (0/0/0)<br>- 이미지 연결 (post_images)<br>- 이미지 TTL 해제<br>- 게시글 상세 페이지 리다이렉트 | HTTP 201 Created<br>- post_id=8 생성<br>- post_stats 초기화 (0/0/0) 확인<br>- 이미지 TTL 해제 확인 | 검증일: 2025-10-18<br>커밋: db3b42f<br>(필드명 수정) |
| 게시글 | 3.3 | 게시글 수정 | - 로그인 완료<br>  (본인 게시글)<br>- 게시글 존재<br>  (post_id=8) | 1. 게시글 상세 페이지에서 "수정" 버튼 클릭<br>2. /board/edit.html?id=8 이동<br>3. 제목 수정: "수정된 게시글 제목"<br>4. 내용 수정: "수정된 내용입니다."<br>5. 이미지 변경 (선택)<br>6. "수정완료" 버튼 클릭<br>7. updatedAt 시간 업데이트 확인<br>8. 게시글 상세 페이지로 리다이렉트 | HTTP 200 OK<br>- 제목/내용 변경 확인<br>- 이미지 변경 확인<br>- updatedAt 시간 업데이트<br>- 작성자만 수정 가능 (403 권한 검증) | HTTP 200 OK<br>- 모든 필드 정상 수정<br>- updatedAt 시간 변경 확인<br>- 권한 검증 정상 | 검증일: 2025-10-18<br>PATCH 부분 업데이트 |
| 게시글 | 3.4 | 게시글 삭제 | - 로그인 완료<br>  (본인 게시글)<br>- 게시글 존재<br>  (post_id=8) | 1. 게시글 상세 페이지에서 "삭제" 버튼 클릭<br>2. 확인 모달 "삭제" 클릭<br>3. DB post_status = DELETED 확인<br>4. /board/list.html 리다이렉트<br>5. 목록에서 제거 확인 | HTTP 204 No Content<br>- DB post_status = DELETED (Soft Delete)<br>- 게시글 목록 리다이렉트<br>- 목록에서 제거됨 | HTTP 204 No Content<br>- Soft Delete 확인<br>- 리다이렉트 성공<br>- 목록에서 제거됨 | 검증일: 2025-10-18<br>Soft Delete 정책 |

---

## 4. 댓글 (Comments)

| 기능 | 번호 | 테스트 케이스 | 전제 조건 | 테스트 순서 | 예상 결과 | 실제 결과 | 비고 |
|------|------|---------------|-----------|-------------|-----------|-----------|------|
| 댓글 | 4.1 | 목록 조회 | - 게시글 존재<br>  (post_id=7) | 1. 게시글 상세 페이지 접속<br>2. GET /posts/7/comments?offset=0&limit=10 호출<br>3. 댓글 목록 렌더링 확인<br>4. 작성일 오름차순 정렬 확인 | HTTP 200 OK<br>- comments 배열 반환<br>- pagination.totalCount 존재<br>- 댓글 렌더링 성공 | HTTP 200 OK<br>- Offset 페이지네이션 정상<br>- 작성일 오름차순 정렬 | 검증일: 2025-10-18<br>Offset 방식 (limit=10) |
| 댓글 | 4.2 | 댓글 작성 | - 로그인 완료<br>- 게시글 상세 페이지 접속<br>  (post_id=7) | 1. 댓글 입력란에 텍스트 입력: "첫 번째 댓글입니다."<br>2. "댓글 작성" 버튼 클릭<br>3. 댓글 목록 최상단에 추가 확인<br>4. commentCount +1 확인 | HTTP 201 Created<br>- commentId 반환<br>- 댓글 목록 최상단 추가<br>- commentCount +1 | HTTP 201 Created<br>- 실시간 댓글 추가 성공<br>- commentCount 정확 업데이트 | 검증일: 2025-10-18<br>커밋: a933570<br>(off-by-one 수정) |
| 댓글 | 4.3 | 댓글 수정 | - 로그인 완료<br>  (본인 댓글)<br>- 댓글 존재<br>  (comment_id=1) | 1. 댓글 "수정" 버튼 클릭<br>2. 댓글 내용 수정: "수정된 댓글입니다."<br>3. "저장" 버튼 클릭<br>4. 댓글 내용 즉시 업데이트 확인<br>5. updatedAt 시간 변경 확인 | HTTP 200 OK<br>- 댓글 내용 즉시 업데이트<br>- updatedAt 시간 변경<br>- 작성자만 수정 가능 (403 권한 검증) | HTTP 200 OK<br>- 댓글 수정 성공<br>- updatedAt 시간 변경 확인 | 검증일: 2025-10-18<br>권한 검증 정상 |
| 댓글 | 4.4 | 댓글 삭제 | - 로그인 완료<br>  (본인 댓글)<br>- 댓글 존재<br>  (comment_id=1) | 1. 댓글 "삭제" 버튼 클릭<br>2. 확인 모달 "삭제" 클릭<br>3. 댓글 목록에서 제거 확인<br>4. commentCount -1 확인<br>5. DB comment_status = DELETED 확인 | HTTP 204 No Content<br>- 댓글 목록에서 제거<br>- commentCount -1<br>- DB comment_status = DELETED (Soft Delete) | HTTP 204 No Content<br>- 댓글 삭제 성공<br>- commentCount 정확 감소 | 검증일: 2025-10-18<br>Soft Delete 정책 |

---

## 5. 이미지 (Images)

| 기능 | 번호 | 테스트 케이스 | 전제 조건 | 테스트 순서 | 예상 결과 | 실제 결과 | 비고 |
|------|------|---------------|-----------|-------------|-----------|-----------|------|
| 이미지 | 5.1 | 이미지 업로드<br>(S3) | - 로그인 완료<br>- 이미지 파일 준비<br>  (JPG/PNG/GIF, 5MB 이하) | 1. /board/write.html 접속<br>2. "이미지 선택" 버튼 클릭<br>3. 이미지 파일 선택<br>4. POST /images 호출 (multipart/form-data)<br>5. S3 업로드 확인<br>6. DB images 테이블 insert 확인<br>7. expires_at = NOW() + 1시간 확인<br>8. 미리보기 이미지 표시 확인 | HTTP 201 Created<br>- imageId, imageUrl 반환<br>- S3 업로드 성공<br>- DB images 테이블 insert<br>- TTL 설정 (1시간)<br>- 파일 검증 (형식, 크기) | HTTP 201 Created<br>- S3 업로드 성공<br>- TTL 설정 확인 (1시간)<br>- 파일 검증 정상 | 검증일: 2025-10-17<br>커밋: de7aea8<br>(필드명 수정) |

---

## 테스트 통계

### 기능별 검증 현황

| 기능 | 전체 API | 검증 완료 | 제외 | 커버리지 |
|------|----------|-----------|------|----------|
| 인증 | 3 | 1 | 2 | 33.3% |
| 사용자 관리 | 5 | 3 | 2 | 60.0% |
| 게시글 | 5 | 4 | 1 | 80.0% |
| 댓글 | 4 | 4 | 0 | **100%** |
| 이미지 | 1 | 1 | 0 | **100%** |
| **합계** | **21** | **13** | **8** | **61.9%** |

---

## 버그 수정 이력

검증 과정에서 발견된 버그 5건 모두 수정 완료:

| 번호 | 심각도 | 내용 | 수정 커밋 |
|------|--------|------|-----------|
| Bug-1 | Critical | JWT 토큰 localStorage 저장 실패 (필드명 불일치) | a49ebcc |
| Bug-2 | Critical | "Entity not managed" 백엔드 버그 (Optimistic Update 패턴 도입) | 8103318 |
| Bug-3 | High | 이미지 업로드 필드명 불일치 (image_id → imageId) | de7aea8, db3b42f |
| Bug-4 | Medium | 비밀번호 변경 필드명 불일치 (new_password → newPassword) | d7c7b61 |
| Bug-5 | Low | 댓글 카운트 off-by-one (state.comments.length ± 1 중복 계산) | a933570 |

---

## 참조 문서

- `VERIFIED_TEST_CASES.md` - 영문 버전 검증 케이스 (상세 제외 사유 포함)
- `docs/test/result/_legacy/` - Legacy 테스트 문서
- `docs/be/API.md` - REST API 명세
- `docs/be/LLD.md` - 백엔드 아키텍처
- `docs/be/DDL.md` - 데이터베이스 스키마

---

**문서 버전**: 1.0
**최종 수정**: 2025-10-19
