# Claude Code 프론트엔드 개발 지침서 (Thymeleaf + JavaScript)

## 핵심 원칙
Thymeleaf 템플릿과 JavaScript를 활용한 프론트엔드 코드는 **변경하기 쉬운 코드**를 목표로 작성한다. 4가지 기준(가독성, 예측 가능성, 응집도, 결합도)을 기반으로 판단한다.

---

## 1. 가독성 (Readability)

### 원칙
템플릿과 JavaScript 코드 모두 위에서 아래로 자연스럽게 읽혀야 하며, 서버/클라이언트 로직을 명확히 구분한다.

### 실무 가이드라인

#### 1.1 Thymeleaf 템플릿 구조화
```html
<!-- ❌ Bad - 복잡한 인라인 조건 -->
<div th:class="${user.status == 'ACTIVE' && user.role == 'ADMIN' ? 'admin-active' : user.status == 'INACTIVE' ? 'inactive' : 'default'}">

<!-- ✅ Good - 변수로 분리 -->
<div th:with="isAdminActive=${user.status == 'ACTIVE' && user.role == 'ADMIN'},
              isInactive=${user.status == 'INACTIVE'}"
     th:class="${isAdminActive ? 'admin-active' : isInactive ? 'inactive' : 'default'}">
```

#### 1.2 JavaScript 모듈화
```javascript
// ❌ Bad - 전역 스코프 오염
var userList = [];
function addUser() { ... }
function deleteUser() { ... }

// ✅ Good - 네임스페이스 패턴
const UserManager = (function() {
  let userList = [];
  
  function addUser(user) { ... }
  function deleteUser(id) { ... }
  
  return {
    add: addUser,
    delete: deleteUser
  };
})();
```

#### 1.3 Fragment 활용
```html
<!-- ❌ Bad - 중복된 구조 -->
<div class="card">
  <h3 th:text="${item1.title}"></h3>
  <p th:text="${item1.description}"></p>
</div>
<div class="card">
  <h3 th:text="${item2.title}"></h3>
  <p th:text="${item2.description}"></p>
</div>

<!-- ✅ Good - Fragment 재사용 -->
<div th:replace="fragments/card :: card(${item1})"></div>
<div th:replace="fragments/card :: card(${item2})"></div>

<!-- fragments/card.html -->
<div th:fragment="card(item)" class="card">
  <h3 th:text="${item.title}"></h3>
  <p th:text="${item.description}"></p>
</div>
```

---

## 2. 예측 가능성 (Predictability)

### 원칙
서버에서 렌더링되는 부분과 클라이언트에서 동작하는 부분을 명확히 구분한다.

### 실무 가이드라인

#### 2.1 데이터 속성 활용
```html
<!-- ❌ Bad - JavaScript에서 텍스트 파싱 -->
<button onclick="deleteUser(this.innerText.split('-')[1])">
  Delete User-123
</button>

<!-- ✅ Good - data 속성 사용 -->
<button th:data-user-id="${user.id}" 
        onclick="deleteUser(this.dataset.userId)">
  Delete User
</button>
```

#### 2.2 이벤트 핸들링 일관성
```javascript
// ❌ Bad - 혼재된 이벤트 바인딩
<button onclick="handleClick()">Click 1</button>
<button id="btn2">Click 2</button>
<script>
  document.getElementById('btn2').onclick = function() { ... }
</script>

// ✅ Good - 일관된 방식 (addEventListener 권장)
<button data-action="delete" data-id="123">Delete</button>
<script>
document.addEventListener('DOMContentLoaded', function() {
  document.querySelectorAll('[data-action="delete"]').forEach(btn => {
    btn.addEventListener('click', handleDelete);
  });
});
</script>
```

#### 2.3 AJAX 응답 표준화
```javascript
// ❌ Bad - 일관성 없는 응답 처리
function saveUser(userData) {
  fetch('/api/users', {...})
    .then(res => res.json())
    .then(data => {
      if(data) location.reload(); // 어떤 데이터?
    });
}

// ✅ Good - 표준화된 응답 처리
function saveUser(userData) {
  fetch('/api/users', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(userData)
  })
  .then(res => res.json())
  .then(response => {
    if (response.success) {
      showMessage(response.message);
      updateUserList(response.data);
    } else {
      showError(response.error);
    }
  });
}
```

---

## 3. 응집도 (Cohesion)

### 원칙
관련된 템플릿, 스타일, 스크립트는 함께 관리한다.

### 실무 가이드라인

#### 3.1 컴포넌트별 파일 구성
```
templates/
└── user/
    ├── list.html         # 사용자 목록 템플릿
    ├── detail.html       # 사용자 상세 템플릿
    └── fragments/        # 사용자 관련 프래그먼트
        ├── userCard.html
        └── userForm.html

static/
├── css/
│   └── user/
│       ├── list.css
│       └── detail.css
└── js/
    └── user/
        ├── list.js
        └── detail.js
```

#### 3.2 관련 설정 그룹화
```javascript
// ✅ Good - 페이지별 설정 객체
const PageConfig = {
  user: {
    api: {
      list: '/api/users',
      detail: '/api/users/{id}',
      save: '/api/users/save'
    },
    messages: {
      saveSuccess: '저장되었습니다.',
      deleteConfirm: '정말 삭제하시겠습니까?'
    },
    validation: {
      nameMinLength: 2,
      nameMaxLength: 50
    }
  }
};
```

#### 3.3 재사용 함수 모듈화
```javascript
// common.js - 공통 유틸리티
const CommonUtils = {
  // 날짜 포맷
  formatDate(date, format = 'YYYY-MM-DD') {
    // 구현
  },
  
  // CSRF 토큰 가져오기
  getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]').content;
  },
  
  // 공통 AJAX 래퍼
  ajax(url, options = {}) {
    const defaultOptions = {
      headers: {
        'X-CSRF-TOKEN': this.getCsrfToken(),
        'Content-Type': 'application/json'
      }
    };
    return fetch(url, {...defaultOptions, ...options});
  }
};
```

---

## 4. 결합도 (Coupling)

### 원칙
JavaScript는 특정 DOM 구조에 과도하게 의존하지 않는다.

### 실무 가이드라인

#### 4.1 선택자 추상화
```javascript
// ❌ Bad - DOM 구조 강한 의존
function updateUserName() {
  const name = document.querySelector('#userForm > div:nth-child(2) > input').value;
  document.querySelector('.content .user-info span:first-child').textContent = name;
}

// ✅ Good - 의미있는 선택자
function updateUserName() {
  const name = document.querySelector('[data-field="userName"]').value;
  document.querySelector('[data-display="userName"]').textContent = name;
}
```

#### 4.2 템플릿-스크립트 분리
```html
<!-- ❌ Bad - 인라인 스크립트 과다 -->
<div th:each="user : ${users}">
  <button th:onclick="'deleteUser(' + ${user.id} + ')'">Delete</button>
  <script th:inline="javascript">
    var userId = [[${user.id}]];
    console.log(userId);
  </script>
</div>

<!-- ✅ Good - 데이터와 동작 분리 -->
<div th:each="user : ${users}" class="user-item">
  <button class="btn-delete" th:data-id="${user.id}">Delete</button>
</div>
<script src="/js/user/list.js"></script>
```

---

## 프로젝트 구조 가이드

### 기본 구조
```
src/main/resources/
├── templates/
│   ├── layout/           # 공통 레이아웃
│   │   ├── default.html
│   │   └── admin.html
│   ├── fragments/        # 재사용 프래그먼트
│   │   ├── header.html
│   │   ├── footer.html
│   │   └── pagination.html
│   └── pages/           # 페이지별 템플릿
│       ├── user/
│       ├── board/
│       └── admin/
└── static/
    ├── css/
    │   ├── common/      # 공통 스타일
    │   └── pages/       # 페이지별 스타일
    ├── js/
    │   ├── common/      # 공통 스크립트
    │   ├── lib/         # 외부 라이브러리
    │   └── pages/       # 페이지별 스크립트
    └── images/
```

### JavaScript 파일 구조 템플릿
```javascript
// pages/user/list.js
(function(window, document) {
  'use strict';
  
  // 설정
  const CONFIG = {
    API_URL: '/api/users',
    PAGE_SIZE: 20
  };
  
  // 상태 관리
  const state = {
    currentPage: 1,
    users: []
  };
  
  // DOM 요소 캐싱
  const elements = {
    userList: null,
    pagination: null,
    searchInput: null
  };
  
  // 초기화
  function init() {
    cacheElements();
    bindEvents();
    loadUsers();
  }
  
  // DOM 요소 캐싱
  function cacheElements() {
    elements.userList = document.querySelector('[data-user-list]');
    elements.pagination = document.querySelector('[data-pagination]');
    elements.searchInput = document.querySelector('[data-search]');
  }
  
  // 이벤트 바인딩
  function bindEvents() {
    elements.searchInput?.addEventListener('input', debounce(handleSearch, 300));
    document.addEventListener('click', handleGlobalClick);
  }
  
  // 전역 클릭 핸들러 (이벤트 위임)
  function handleGlobalClick(e) {
    if (e.target.matches('[data-action="delete"]')) {
      handleDelete(e.target.dataset.id);
    }
  }
  
  // DOMContentLoaded
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
  
})(window, document);
```

---

## 코드 리뷰 체크리스트

### Thymeleaf 체크리스트
- [ ] Fragment로 분리 가능한 중복 코드가 있는가?
- [ ] 복잡한 조건은 th:with로 변수화했는가?
- [ ] th:inline="javascript" 사용을 최소화했는가?
- [ ] 레이아웃 상속을 적절히 활용했는가?

### JavaScript 체크리스트
- [ ] 전역 변수 사용을 최소화했는가?
- [ ] DOM 조작이 최소화되었는가?
- [ ] 이벤트 위임을 활용했는가?
- [ ] AJAX 에러 처리가 적절한가?
- [ ] CSRF 토큰이 포함되었는가?

### 통합 체크리스트
- [ ] 서버/클라이언트 책임이 명확한가?
- [ ] 데이터 속성(data-*)을 활용했는가?
- [ ] 캐싱 가능한 정적 리소스는 분리했는가?
- [ ] 페이지별 JS/CSS가 적절히 분리되었는가?

---

## 실무 적용 우선순위

### 상황별 가이드
1. **신규 페이지 개발 시**
   - Fragment 설계 > JavaScript 모듈화 > 스타일 분리

2. **기존 페이지 개선 시**
   - 인라인 스크립트 제거 > 이벤트 위임 적용 > Fragment 분리

3. **성능 개선 시**
   - DOM 조작 최소화 > 정적 리소스 캐싱 > AJAX 요청 최적화

---

## 즉시 적용 가능한 규칙

### 🔴 필수 규칙 (반드시 준수)
1. **인라인 스크립트 최소화**
   - th:onclick 대신 data 속성 + addEventListener 사용
2. **CSRF 토큰 포함**
   - 모든 POST/PUT/DELETE 요청에 포함
3. **전역 변수 사용 금지**
   - 네임스페이스 또는 모듈 패턴 사용

### 🟡 권장 규칙 (가능한 준수)
1. **Fragment 기준**
   - 3번 이상 반복되는 HTML은 Fragment로 분리
   - 50줄 이상의 복잡한 구조는 분리 검토

2. **JavaScript 파일 크기**
   - 페이지별 JS: 300줄 이하 권장
   - 공통 모듈: 200줄 이하 권장
   - 복잡한 기능: 500줄까지 허용

3. **선택자 성능**
   - ID > Class > Data 속성 > 태그 순으로 사용
   - 깊이 3단계 이상의 선택자 지양

### 🟢 상황별 유연 적용
1. **jQuery 사용**
   - 기존 프로젝트: 일관성 유지
   - 신규 프로젝트: Vanilla JS 권장

2. **템플릿 엔진 기능**
   - 단순 조건: th:if/th:unless 사용
   - 복잡한 로직: Controller에서 처리

3. **정적 리소스 번들링**
   - 개발: 파일 분리 유지
   - 운영: 번들링/압축 적용