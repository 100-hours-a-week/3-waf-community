const express = require('express');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

// origin_source/static 디렉토리만 정적 파일로 서빙
app.use(express.static(path.join(__dirname, 'origin_source/static')));

// 루트 경로 접속 시 로그인 페이지로 리다이렉트
app.get('/', (req, res) => {
  res.redirect('/pages/home/index.html');
});

// 404 처리
app.use((req, res) => {
  res.status(404).send(`
    <h1>404 - 페이지를 찾을 수 없습니다</h1>
    <p>요청한 경로: ${req.path}</p>
    <a href="/">홈으로 돌아가기</a>
  `);
});

// 서버 시작
app.listen(PORT, () => {
  console.log('='.repeat(50));
  console.log('✅ KTB 커뮤니티 프론트엔드 서버 실행 중');
  console.log('='.repeat(50));
  console.log(`🌐 로컬 주소:     http://localhost:${PORT}`);
  console.log(`📁 정적 파일:     origin_source/static/`);
  console.log(`🔗 백엔드 API:    http://localhost:8080 (Spring Boot)`);
  console.log(`📄 로그인 페이지: http://localhost:${PORT}/pages/user/login.html`);
  console.log('='.repeat(50));
});
