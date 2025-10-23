# KTB 커뮤니티 프론트엔드

Vanilla JS, HTML, CSS로 작성된 커뮤니티 플랫폼 프론트엔드입니다.

## 시작하기

### 1. 의존성 설치
```bash
npm install
```

### 2. 서버 실행

**개발 모드 (자동 재시작):**
```bash
npm run dev
```

**운영 모드:**
```bash
npm start
```

### 3. 접속
브라우저에서 다음 주소로 접속:
- **로그인 페이지**: http://localhost:3000/static/pages/user/login.html
- **게시글 목록**: http://localhost:3000/static/pages/board/list.html

## 프로젝트 구조

```
ktb_community_fe/
├── origin_source/          # 프론트엔드 소스 파일
│   ├── static/
│   │   ├── css/           # 스타일시트
│   │   ├── js/            # JavaScript 파일
│   │   └── pages/         # HTML 페이지
│   └── test_image/        # 테스트 이미지
├── server.js              # Express 서버
└── package.json           # 프로젝트 설정
```

## 백엔드 연동

프론트엔드는 Spring Boot 백엔드 API를 사용합니다.

**백엔드 서버**: http://localhost:8080

백엔드 서버가 실행 중이어야 로그인, 회원가입 등의 기능이 작동합니다.

## 포트 변경

기본 포트는 3000입니다. 변경하려면:

```bash
PORT=8000 npm start
```

## 기술 스택

- **프론트엔드**: Vanilla JavaScript, HTML, CSS
- **서버**: Express.js (정적 파일 서빙)
- **백엔드**: Spring Boot 3.5.6 (별도 프로젝트)
