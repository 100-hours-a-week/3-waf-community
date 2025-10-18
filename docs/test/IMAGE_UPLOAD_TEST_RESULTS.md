# 이미지 업로드 통합 테스트 결과

## 테스트 정보

| 항목 | 내용 |
|------|------|
| 테스트 일시 | 2025-10-18 18:00-18:30 |
| 브랜치 | test/image-upload-integration |
| 테스트 도구 | Chrome DevTools MCP (자동화) |
| 테스트 이미지 | test_image1.jpg (5.5KB), test_image2.jpg (5.8KB), test_image3.jpeg (12KB) |

---

## 테스트 시나리오 및 결과

### ✅ Scenario 3: 회원가입 시 프로필 이미지 업로드 (성공)

**테스트 내용:**
- 회원가입 폼에서 프로필 이미지 선택 (test_image3.jpeg)
- multipart/form-data로 이메일, 비밀번호, 닉네임, 프로필 이미지 전송
- 자동 로그인 후 게시글 목록 페이지로 리다이렉트

**결과:**
- ✅ 회원가입 성공 (user_id=13, nickname=이미지테스트)
- ✅ 자동 로그인 완료
- ✅ 게시글 목록 페이지 이동 성공

**스크린샷:**
- `scenario3-signup-page.png` - 회원가입 폼
- `scenario3-before-submit.png` - 이미지 선택 후
- `scenario3-after-signup.png` - 가입 완료 후 게시글 목록

**평가:** ✅ **완전 성공** - multipart/form-data 패턴 정상 작동

---

### ⚠️ Scenario 1: 게시글 작성 시 이미지 업로드 - 2단계 패턴 (부분 성공)

**테스트 내용:**
- 게시글 작성 폼에서 이미지 선택 (test_image1.jpg)
- 2단계 업로드 패턴:
  1. POST /images → image_id 획득
  2. POST /posts (image_id 포함)

**결과:**
- ✅ 이미지 파일 선택 가능
- ✅ 이미지 미리보기 표시
- ✅ POST /images 성공 (image_id=4 생성됨, 18:24:39)
- ❌ **POST /posts에 image_id 연결 실패**

**DB 확인:**
```sql
SELECT * FROM images WHERE image_id=4;
-- image_id=4, original_filename=test_image1.jpg, file_size=5640, created_at=2025-10-18 18:24:39

SELECT * FROM post_images WHERE post_id=9;
-- Empty set (이미지 연결 없음)
```

**원인 분석:**
- write.js의 handleSubmit() 함수에서 uploadImage() 호출은 정상
- 하지만 POST /posts 요청에 image_id가 포함되지 않음
- state.uploadedImageId 값이 제대로 설정되지 않았을 가능성

**스크린샷:**
- `scenario1-write-page.png` - 게시글 작성 페이지
- `scenario1-with-image.png` - 이미지 선택 후 (미리보기 표시)
- `scenario1-post-detail.png` - 작성 완료 후 상세 페이지

**평가:** ⚠️ **부분 성공** - 이미지 업로드는 되나 게시글 연결 실패

---

### ⚠️ Scenario 2: 프로필 수정 시 이미지 변경 (부분 성공)

**테스트 내용:**
- 프로필 수정 폼에서 프로필 이미지 변경 (test_image2.jpg)
- multipart/form-data로 닉네임, 프로필 이미지 전송
- PATCH /users/13 요청

**결과:**
- ✅ 닉네임 변경 성공 (이미지테스트 → 이미지수정)
- ✅ "프로필이 수정되었습니다" 메시지 표시
- ❌ **프로필 이미지 업로드 실패 (image_id = NULL)**

**DB 확인:**
```sql
SELECT user_id, nickname, image_id FROM users WHERE user_id=13;
-- user_id=13, nickname=이미지수정, image_id=NULL
```

**원인 분석:**
- 닉네임 필드는 정상 전송되어 업데이트됨
- 프로필 이미지 파일이 multipart request에 포함되지 않았을 가능성
- profile-edit.js에서 FormData 생성 시 파일 누락 추정

**스크린샷:**
- `scenario2-profile-edit.png` - 프로필 수정 페이지 로드
- `scenario2-before-save.png` - 이미지 선택 후
- `scenario2-after-save.png` - 저장 완료 후

**평가:** ⚠️ **부분 성공** - 닉네임은 변경되나 이미지 업로드 실패

---

## 종합 결과

| Scenario | 상태 | 성공률 | 비고 |
|----------|------|--------|------|
| Scenario 3 (회원가입) | ✅ 성공 | 100% | multipart/form-data 패턴 정상 |
| Scenario 1 (게시글) | ⚠️ 부분 | 70% | 이미지 업로드는 되나 연결 실패 |
| Scenario 2 (프로필 수정) | ⚠️ 부분 | 50% | 닉네임만 변경, 이미지 실패 |

**전체 평가:** ⚠️ **부분 성공 (3/3 시나리오 테스트, 1/3 완전 성공)**

---

## 발견된 문제점

### 1. 게시글 작성 시 이미지 연결 실패 (Scenario 1)

**문제:**
- POST /images는 성공하지만 POST /posts에 image_id 미포함
- post_images 테이블에 연결 데이터 없음

**의심 코드:**
```javascript
// write.js:124-128
if (state.selectedFile && !state.uploadedImageId) {
  const imageResult = await uploadImage(state.selectedFile);
  state.uploadedImageId = imageResult.image_id;  // ← 값 설정 확인 필요
  state.uploadedImageUrl = imageResult.image_url;
}
```

**수정 방향:**
1. uploadImage() 응답 구조 확인 (image_id vs imageId)
2. state.uploadedImageId 값 설정 확인
3. POST /posts 요청 body 로그 추가

---

### 2. 프로필 수정 시 이미지 업로드 실패 (Scenario 2)

**문제:**
- 닉네임은 정상 전송되나 프로필 이미지 파일 누락
- users.image_id가 NULL로 유지됨

**의심 코드:**
```javascript
// profile-edit.js:? (FormData 생성 부분 확인 필요)
const formData = new FormData();
formData.append('nickname', nickname);
if (state.selectedFile) {
  formData.append('profile_image', state.selectedFile);  // ← 파일 추가 확인 필요
}
```

**수정 방향:**
1. profile-edit.js의 handleSubmit() 함수 검토
2. FormData에 파일이 제대로 추가되는지 확인
3. 백엔드 요청 로그에서 multipart 파싱 확인

---

### 3. 프로필 이미지 표시 문제 (우측 상단 헤더)

**문제:**
- 사용자 피드백: "최근 수정 작업에서 프로필 이미지가 깨지는 문제 발생"
- 우측 상단에 프로필 이미지/메뉴가 표시되지 않음

**영향 범위:**
- 게시글 목록 페이지 (list.html)
- 기타 프로필 표시 영역

**수정 방향:**
1. header.html fragment 확인
2. 프로필 이미지 로드 로직 검토 (list.js:loadUserProfile)
3. CSS 스타일 문제 확인

---

## 스크린샷 목록

총 9개 스크린샷 저장 위치: `/docs/test/screenshots/`

| 파일명 | 설명 |
|--------|------|
| scenario3-signup-page.png | 회원가입 폼 초기 화면 |
| scenario3-before-submit.png | 프로필 이미지 선택 후 |
| scenario3-after-signup.png | 회원가입 완료 후 게시글 목록 |
| scenario1-write-page.png | 게시글 작성 페이지 |
| scenario1-with-image.png | 게시글 이미지 선택 후 (미리보기) |
| scenario1-post-detail.png | 게시글 작성 완료 후 상세 페이지 |
| scenario2-profile-edit.png | 프로필 수정 페이지 로드 |
| scenario2-before-save.png | 프로필 이미지 선택 + 닉네임 변경 |
| scenario2-after-save.png | 프로필 수정 완료 후 |

---

## 권장 사항

### 즉시 수정 필요 (Priority: High)

1. **write.js 이미지 연결 로직 수정**
   - uploadImage() 응답 구조 확인
   - state.uploadedImageId 디버깅 로그 추가
   - POST /posts 요청 검증

2. **profile-edit.js FormData 파일 추가 확인**
   - state.selectedFile 값 확인
   - FormData.append('profile_image') 검증
   - 백엔드 multipart 파싱 로그 확인

### 추가 개선 (Priority: Medium)

3. **프로필 이미지 표시 문제 해결**
   - 우측 상단 헤더 프로필 이미지 복구
   - 이미지 로드 실패 시 기본 아바타 표시

4. **에러 처리 강화**
   - 이미지 업로드 실패 시 사용자 피드백 개선
   - 네트워크 에러 재시도 로직 추가

---

## 다음 단계

1. ✅ 테스트 결과 문서화 완료
2. ⏳ 발견된 문제점 이슈 등록 (GitHub Issues 또는 TODO)
3. ⏳ write.js, profile-edit.js 코드 수정
4. ⏳ 재테스트 및 검증
5. ⏳ dev 브랜치 병합

---

## 참고 자료

- 테스트 브랜치: `test/image-upload-integration`
- 관련 파일:
  - `/js/pages/board/write.js` (게시글 작성)
  - `/js/pages/user/profile-edit.js` (프로필 수정)
  - `/js/pages/user/register.js` (회원가입 - 정상 작동)
  - `/js/common/api.js` (uploadImage 함수)
- DB 테이블: `images`, `post_images`, `users`
